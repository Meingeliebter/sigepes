package com.sigepes.backend.trips;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;

/**
 * RF-03 / RF-05 / RF-06 / RF-08 · Reglas críticas de la bitácora: permiso, veda, talla mínima y
 * cierre de faena.
 */
class TripRulesTest extends ApiTestSupport {

    private static final String ZONE = "Pacífico Norte";

    private UserEntity fisher;
    private VesselEntity vessel;
    private SpeciesEntity snapper;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        fisher = givenUser("1010101010", "pescador.test@sigepes.gov.co", Role.FISHER);
        FishingGearEntity handline = givenGear("Línea de mano test", false);
        vessel = givenVessel(fisher, "DIMAR-TEST-001", VesselStatus.ACTIVE, handline);
        snapper = givenSpecies("Pargo test", new BigDecimal("35.00"));
        token = bearerFor("pescador.test@sigepes.gov.co");
    }

    // ------------------------------------------------------------------
    // RN-16 · permiso
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Abre la faena con permiso vigente")
    void startsTripWithValidPermit() throws Exception {
        givenValidPermit();

        startTrip().andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.vessel").value(vessel.getName()))
                .andExpect(jsonPath("$.permitNumber").value("PES-TEST-1"));
    }

    @Test
    @DisplayName("RN-16: bloquea la faena sin ningún permiso")
    void blocksTripWithoutPermit() throws Exception {
        startTrip().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMIT_REQUIRED"));
    }

    @Test
    @DisplayName("RN-16: bloquea la faena con permiso vencido")
    void blocksTripWithExpiredPermit() throws Exception {
        givenPermit(fisher, "PES-TEST-EXP", PermitStatus.APPROVED, LocalDate.now().minusYears(2),
                LocalDate.now().minusMonths(1));

        startTrip().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMIT_EXPIRED"));
    }

    @Test
    @DisplayName("RN-16: bloquea la faena con permiso rechazado")
    void blocksTripWithRejectedPermit() throws Exception {
        givenPermit(fisher, "PES-TEST-REJ", PermitStatus.REJECTED, LocalDate.now().minusDays(5),
                LocalDate.now().plusYears(1));

        startTrip().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMIT_REQUIRED"));
    }

    @Test
    @DisplayName("RN-16: bloquea la faena con permiso suspendido")
    void blocksTripWithSuspendedPermit() throws Exception {
        givenPermit(fisher, "PES-TEST-SUS", PermitStatus.SUSPENDED, LocalDate.now().minusDays(5),
                LocalDate.now().plusYears(1));

        startTrip().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMIT_SUSPENDED"));
    }

    @Test
    @DisplayName("Excepción de subsistencia: permite operar con permiso de subsistencia caducado")
    void allowsSubsistence() throws Exception {
        givenPermit(fisher, "PES-TEST-SUB", PermitStatus.APPROVED, LocalDate.now().minusYears(3),
                LocalDate.now().minusYears(1), "SUBSISTENCIA");

        startTrip().andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Bloquea la faena si la embarcación está en mantenimiento")
    void blocksInactiveVessel() throws Exception {
        givenValidPermit();
        VesselEntity broken = givenVessel(fisher, "DIMAR-TEST-002", VesselStatus.UNDER_MAINTENANCE);

        mockMvc.perform(post("/api/v1/faenas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"%s"}""".formatted(broken.getId(), ZONE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VESSEL_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("Bloquea la faena con una embarcación de otro usuario")
    void blocksForeignVessel() throws Exception {
        givenValidPermit();
        UserEntity other = givenUser("2020202020", "otro@sigepes.gov.co", Role.FISHER);
        VesselEntity foreign = givenVessel(other, "DIMAR-TEST-003", VesselStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/faenas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"%s"}""".formatted(foreign.getId(), ZONE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VESSEL_NOT_OWNED"));
    }

    // ------------------------------------------------------------------
    // RN-12 y RN-10 · talla mínima y veda
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Registra una captura válida")
    void registersValidCatch() throws Exception {
        givenValidPermit();
        String tripId = startedTripId();

        addCatch(tripId, snapper.getId(), "40.0", "120.5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captures.length()").value(1))
                .andExpect(jsonPath("$.captures[0].species").value("Pargo test"))
                .andExpect(jsonPath("$.totalWeightKg").value(120.5));
    }

    @Test
    @DisplayName("RN-12: rechaza una captura por debajo de la talla mínima")
    void rejectsUndersizedCatch() throws Exception {
        givenValidPermit();
        String tripId = startedTripId();

        addCatch(tripId, snapper.getId(), "20.0", "50.0")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MINIMUM_SIZE_VIOLATION"));
    }

    @Test
    @DisplayName("Registra captura de una especie cuya veda no cubre la zona")
    void allowsCatchOutsideClosure() throws Exception {
        givenValidPermit();
        givenClosure(snapper, "Caribe", LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        String tripId = startedTripId();

        addCatch(tripId, snapper.getId(), "40.0", "80.0").andExpect(status().isOk());
    }

    @Test
    @DisplayName("RN-10: rechaza captura de una especie en veda vigente para la zona")
    void rejectsCatchUnderClosure() throws Exception {
        givenValidPermit();
        givenClosure(snapper, ZONE, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        String tripId = startedTripId();

        addCatch(tripId, snapper.getId(), "40.0", "80.0")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SPECIES_UNDER_CLOSURE"));
    }

    @Test
    @DisplayName("RN-10: una veda con zona TODAS bloquea cualquier zona")
    void rejectsCatchUnderGlobalClosure() throws Exception {
        givenValidPermit();
        givenClosure(snapper, "TODAS", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        String tripId = startedTripId();

        addCatch(tripId, snapper.getId(), "40.0", "80.0")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SPECIES_UNDER_CLOSURE"));
    }

    // ------------------------------------------------------------------
    // RN-06 · cierre de faena
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RN-06: cierra la faena con capturas registradas")
    void closesTripWithCatches() throws Exception {
        givenValidPermit();
        String tripId = startedTripId();
        addCatch(tripId, snapper.getId(), "40.0", "100.0").andExpect(status().isOk());

        closeTrip(tripId, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.nullDeclaration").value(false));
    }

    @Test
    @DisplayName("RN-06: impide cerrar una faena vacía sin declaración")
    void preventsClosingEmptyTrip() throws Exception {
        givenValidPermit();
        String tripId = startedTripId();

        closeTrip(tripId, false)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRIP_CANNOT_CLOSE"));
    }

    @Test
    @DisplayName("RN-06: permite cerrar con declaración de captura nula")
    void allowsNullDeclaration() throws Exception {
        givenValidPermit();
        String tripId = startedTripId();

        closeTrip(tripId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.nullDeclaration").value(true));
    }

    @Test
    @DisplayName("RN-06: no admite declaración nula si ya hay capturas")
    void rejectsNullDeclarationWithCatches() throws Exception {
        givenValidPermit();
        String tripId = startedTripId();
        addCatch(tripId, snapper.getId(), "40.0", "100.0").andExpect(status().isOk());

        closeTrip(tripId, true)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NULL_DECLARATION_WITH_CATCHES"));
    }

    @Test
    @DisplayName("No admite capturas en una faena ya cerrada")
    void rejectsCatchOnClosedTrip() throws Exception {
        givenValidPermit();
        String tripId = startedTripId();
        closeTrip(tripId, true).andExpect(status().isOk());

        addCatch(tripId, snapper.getId(), "40.0", "100.0")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_ALREADY_CLOSED"));
    }

    @Test
    @DisplayName("La sincronización es idempotente por identificador")
    void synchronizesWithoutDuplicating() throws Exception {
        givenValidPermit();
        String body = """
                [{"id":"LOCAL-1","vesselId":"%s","zone":"%s","status":"CLOSED","nullDeclaration":true,
                  "captures":[]}]""".formatted(vessel.getId(), ZONE);

        mockMvc.perform(post("/api/v1/faenas/sincronizar").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("LOCAL-1"))
                .andExpect(jsonPath("$[0].status").value("CLOSED"));

        mockMvc.perform(post("/api/v1/faenas/sincronizar").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(1,
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/faenas").header("Authorization", token))
                        .andReturn().getResponse().getContentAsString().split("\"id\":\"LOCAL-1\"", -1).length - 1);
    }

    // ------------------------------------------------------------------

    private void givenValidPermit() {
        givenPermit(fisher, "PES-TEST-1", PermitStatus.APPROVED, LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(6));
    }

    private org.springframework.test.web.servlet.ResultActions startTrip() throws Exception {
        return mockMvc.perform(post("/api/v1/faenas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"%s"}""".formatted(vessel.getId(), ZONE)));
    }

    private String startedTripId() throws Exception {
        String response = startTrip().andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extract(response, "id");
    }

    private org.springframework.test.web.servlet.ResultActions addCatch(String tripId, String speciesId, String size,
            String weight) throws Exception {
        return mockMvc.perform(post("/api/v1/faenas/" + tripId + "/capturas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","weightKg":%s,"averageSizeCm":%s}"""
                        .formatted(speciesId, weight, size)));
    }

    private org.springframework.test.web.servlet.ResultActions closeTrip(String tripId, boolean nullDeclaration)
            throws Exception {
        return mockMvc.perform(post("/api/v1/faenas/" + tripId + "/cerrar").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nullDeclaration":%s}""".formatted(nullDeclaration)));
    }
}
