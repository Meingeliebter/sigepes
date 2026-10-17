package com.sigepes.backend.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.sigepes.backend.common.Ids;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;
import com.sigepes.backend.common.AppClock;

/** HU-04 · RF-04 / RN-08 · Geolocalización de la zona de pesca y zonas restringidas. */
@RecordApplicationEvents
class GeoRulesTest extends ApiTestSupport {

    private static final String SQUARE = "[[6.0,-77.6],[6.4,-77.6],[6.4,-77.4],[6.0,-77.4]]";

    @Autowired
    private RestrictedZoneRepository zoneRepository;

    @Autowired
    private TripPositionRepository positionRepository;

    @Autowired
    private ZoneEventRepository eventRepository;

    @Autowired
    private ApplicationEvents events;

    private VesselEntity vessel;
    private String fisherToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        UserEntity fisher = givenUser("5050505050", "pescador.geo@sigepes.gov.co", Role.FISHER);
        givenUser("6060606060", "inspector.geo@sigepes.gov.co", Role.INSPECTOR);
        vessel = givenVessel(fisher, "DIMAR-GEO-001", VesselStatus.ACTIVE, givenGear("Línea geo", false));
        givenPermit(fisher, "PES-GEO-1", PermitStatus.APPROVED, AppClock.today().minusMonths(1),
                AppClock.today().plusMonths(6));
        fisherToken = bearerFor("pescador.geo@sigepes.gov.co");
        inspectorToken = bearerFor("inspector.geo@sigepes.gov.co");
    }

    @Test
    @DisplayName("TC-HU04-02 · El inspector registra una zona con polígono y referencia legal")
    void inspectorCreatesZone() throws Exception {
        createZone(inspectorToken, SQUARE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.polygon.length()").value(4))
                .andExpect(jsonPath("$.resolutionNumber").value("AUNAP-GEO-0001"));
    }

    @Test
    @DisplayName("TC-HU04-02 · Rechaza un polígono que se cruza a sí mismo")
    void rejectsSelfIntersectingPolygon() throws Exception {
        createZone(inspectorToken, "[[6.0,-77.6],[6.4,-77.4],[6.4,-77.6],[6.0,-77.4]]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ZONE_POLYGON"));
    }

    @Test
    @DisplayName("AC-04.2 · Un pescador no puede registrar zonas")
    void fisherCannotCreateZone() throws Exception {
        createZone(fisherToken, SQUARE).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-HU04-01 · Registra las posiciones de la faena y las devuelve en orden")
    void recordsPositionsInOrder() throws Exception {
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), "Pacífico Norte");
        Instant start = Instant.now().minusSeconds(1200);
        String body = """
                {"positions":[
                  {"id":"pos-t1-b","latitude":6.70,"longitude":-77.10,"accuracyMeters":8,"recordedAt":"%s"},
                  {"id":"pos-t1-a","latitude":6.71,"longitude":-77.11,"accuracyMeters":9,"recordedAt":"%s"}
                ]}""".formatted(start.plusSeconds(300), start);

        postPositions(tripId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stored").value(2))
                .andExpect(jsonPath("$.intrusions.length()").value(0));

        mockMvc.perform(get("/api/v1/faenas/" + tripId + "/posiciones").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pos-t1-a"))
                .andExpect(jsonPath("$[1].id").value("pos-t1-b"));
    }

    @Test
    @DisplayName("TC-HU04-03 · RN-08: una posición dentro de la zona crea el evento de control y la alerta")
    void intrusionCreatesControlEvent() throws Exception {
        givenZone();
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), "Pacífico Norte");

        postPositions(tripId, """
                {"positions":[{"id":"pos-in-1","latitude":6.2,"longitude":-77.5}]}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intrusions.length()").value(1))
                .andExpect(jsonPath("$.intrusions[0].zone").value("Zona geo test"));

        assertThat(eventRepository.findByTripIdOrderByOccurredAtAsc(tripId)).hasSize(1);
        assertThat(events.stream(RestrictedZoneEntered.class)).singleElement()
                .satisfies(event -> assertThat(event.zoneName()).isEqualTo("Zona geo test"));
        mockMvc.perform(get("/api/v1/eventos-zona").header("Authorization", inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(tripId));
    }

    @Test
    @DisplayName("TC-HU04-04 · AC-04.6: un lote de 12 posiciones offline se guarda completo y reintentarlo no duplica")
    void offlineBatchIsIdempotent() throws Exception {
        givenZone();
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), "Pacífico Norte");
        String positions = IntStream.range(0, 12)
                .mapToObj(i -> """
                        {"id":"pos-off-%d","latitude":%s,"longitude":-77.5}""".formatted(i, i == 5 ? "6.2" : "6.9"))
                .collect(Collectors.joining(","));
        String body = "{\"positions\":[" + positions + "]}";

        postPositions(tripId, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.stored").value(12))
                .andExpect(jsonPath("$.intrusions.length()").value(1));
        postPositions(tripId, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.stored").value(0))
                .andExpect(jsonPath("$.duplicates").value(12));

        assertThat(positionRepository.findByTripIdOrderByRecordedAtAsc(tripId)).hasSize(12);
        assertThat(eventRepository.findByTripIdOrderByOccurredAtAsc(tripId)).hasSize(1);
    }

    @Test
    @DisplayName("RN-08 · Varias posiciones seguidas dentro de la zona son un solo ingreso; salir y volver a entrar es otro")
    void consecutivePositionsInsideAreOneEntry() throws Exception {
        givenZone();
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), "Pacífico Norte");
        Instant t = Instant.now().minusSeconds(3600);
        postPositions(tripId, """
                {"positions":[
                  {"id":"pos-e-1","latitude":6.2,"longitude":-77.5,"recordedAt":"%s"},
                  {"id":"pos-e-2","latitude":6.25,"longitude":-77.5,"recordedAt":"%s"},
                  {"id":"pos-e-3","latitude":6.9,"longitude":-77.5,"recordedAt":"%s"},
                  {"id":"pos-e-4","latitude":6.3,"longitude":-77.5,"recordedAt":"%s"}
                ]}""".formatted(t, t.plusSeconds(300), t.plusSeconds(600), t.plusSeconds(900)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intrusions.length()").value(2));
    }

    @Test
    @DisplayName("Solo el titular registra posiciones de su faena")
    void onlyOwnerRecordsPositions() throws Exception {
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), "Pacífico Norte");
        givenUser("7070707070", "otro.geo@sigepes.gov.co", Role.FISHER);
        String other = bearerFor("otro.geo@sigepes.gov.co");

        mockMvc.perform(post("/api/v1/faenas/" + tripId + "/posiciones").header("Authorization", other)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"positions":[{"latitude":6.2,"longitude":-77.5}]}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-04.5 · Punto en polígono: dentro, fuera y en una zona cóncava")
    void pointInPolygon() {
        Polygon concave = Polygon.validated(List.of(new double[] { 0, 0 }, new double[] { 0, 10 },
                new double[] { 10, 10 }, new double[] { 5, 5 }, new double[] { 10, 0 }));
        assertThat(concave.contains(2, 5)).isTrue();
        assertThat(concave.contains(8, 5)).isFalse();
        assertThat(concave.contains(20, 20)).isFalse();
    }

    private void givenZone() {
        zoneRepository.save(new RestrictedZoneEntity(Ids.generate("zon"), "Zona geo test", ZoneType.CLOSURE_ZONE,
                Polygon.validated(List.of(new double[] { 6.0, -77.6 }, new double[] { 6.4, -77.6 },
                        new double[] { 6.4, -77.4 }, new double[] { 6.0, -77.4 })),
                "AUNAP-GEO-0001"));
    }

    private org.springframework.test.web.servlet.ResultActions createZone(String token, String polygon)
            throws Exception {
        return mockMvc.perform(post("/api/v1/zonas-restringidas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Zona nueva","zoneType":"CLOSURE_ZONE","polygon":%s,"resolutionNumber":"AUNAP-GEO-0001"}"""
                        .formatted(polygon)));
    }

    private org.springframework.test.web.servlet.ResultActions postPositions(String tripId, String body)
            throws Exception {
        return mockMvc.perform(post("/api/v1/faenas/" + tripId + "/posiciones").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
