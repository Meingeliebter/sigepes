package com.sigepes.backend.traceability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;
import com.sigepes.backend.common.AppClock;

import jakarta.persistence.EntityManager;

/** HU-09 · RF-09 / RN-18 · Trazabilidad de la cadena de valor. */
class TraceabilityRulesTest extends ApiTestSupport {

    private static final String ZONE = "Caribe";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private SpeciesEntity snapper;
    private String fisherToken;
    private String otherToken;
    private String tripId;
    private String catchId;

    @BeforeEach
    void setUp() throws Exception {
        UserEntity fisher = givenUser("2121212121", "pescador.lote@sigepes.gov.co", Role.FISHER);
        givenUser("2121212122", "otro.lote@sigepes.gov.co", Role.FISHER);
        VesselEntity vessel = givenVessel(fisher, "DIMAR-LOT-001", VesselStatus.ACTIVE, givenGear("Línea lote", false));
        snapper = givenSpecies("Pargo lote", new BigDecimal("35.00"));
        givenPermit(fisher, "PES-LOT-1", PermitStatus.APPROVED, AppClock.today().minusMonths(1),
                AppClock.today().plusMonths(6));
        fisherToken = bearerFor("pescador.lote@sigepes.gov.co");
        otherToken = bearerFor("otro.lote@sigepes.gov.co");

        tripId = apiStartedTripId(fisherToken, vessel.getId(), ZONE);
        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "120").andExpect(status().isOk());
        apiCloseTrip(fisherToken, tripId).andExpect(status().isOk());
        String eligible = mockMvc.perform(get("/api/v1/faenas/" + tripId + "/capturas-para-lote")
                .header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        catchId = extract(eligible, "catchId");
    }

    @Test
    @DisplayName("TC-HU09-01 · Lote desde una captura legal: código, verificaciones y primer eslabón")
    void createsLotFromLegalCatch() throws Exception {
        mockMvc.perform(get("/api/v1/faenas/" + tripId + "/capturas-para-lote").header("Authorization", fisherToken))
                .andExpect(jsonPath("$[0].legal").value(true))
                .andExpect(jsonPath("$[0].availableKg").value(120.0));

        createLot("80")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", Matchers.matchesPattern("LOT-[2-9A-HJKMNP-Z]{8}")))
                .andExpect(jsonPath("$.legalChecks.length()").value(5))
                .andExpect(jsonPath("$.chain.length()").value(1))
                .andExpect(jsonPath("$.chain[0].type").value("CREATED"))
                .andExpect(jsonPath("$.integrityOk").value(true));

        // Solo quedan 40 kg: no se puede vender dos veces el mismo pescado.
        createLot("50")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOT_WEIGHT_EXCEEDED"));
    }

    @Test
    @DisplayName("TC-HU09-02 · RN-18: una veda declarada sobre la fecha de la faena bloquea el lote")
    void closureBlocksLot() throws Exception {
        givenClosure(snapper, ZONE, AppClock.today().minusDays(1), AppClock.today().plusDays(10));

        createLot("50")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LOT_NOT_LEGAL"))
                .andExpect(jsonPath("$.message", Matchers.containsString("veda")));
    }

    @Test
    @DisplayName("TC-HU09-03 · Solo el titular de la faena crea lotes de su captura")
    void onlyOwnerCreatesLots() throws Exception {
        mockMvc.perform(post("/api/v1/lotes").header("Authorization", otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"catchId":"%s","weightKg":10}""".formatted(catchId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-HU09-04 · Transferencias encadenadas y verificación pública sin datos personales")
    void transfersAndPublicVerification() throws Exception {
        String body = createLot("100").andReturn().getResponse().getContentAsString();
        String lotId = extract(body, "id");
        String code = extract(body, "code");

        transfer(lotId, "BUYER", "Pesquera del Pacífico SAS", "900123456-7", "Bahía Solano")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chain.length()").value(2))
                .andExpect(jsonPath("$.currentHolder").value("Pesquera del Pacífico SAS"));
        transfer(lotId, "TRADER", "Mercado Central Medellín", null, "Medellín")
                .andExpect(jsonPath("$.integrityOk").value(true));

        String publicBody = mockMvc.perform(get("/api/v1/trazabilidad/" + code.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legal").value(true))
                .andExpect(jsonPath("$.integrityOk").value(true))
                .andExpect(jsonPath("$.chain.length()").value(3))
                .andExpect(jsonPath("$.chain[2].partyType").value("TRADER"))
                .andReturn().getResponse().getContentAsString();
        // AC-09.4: ni el pescador ni los NIT o razones sociales salen en la página pública.
        org.assertj.core.api.Assertions.assertThat(publicBody)
                .doesNotContain("Usuario FISHER", "2121212121", "900123456-7", "Pesquera del Pacífico", "ownerName",
                        "PES-LOT-1");
    }

    @Test
    @DisplayName("TC-HU09-05 · AC-09.5: alterar el peso de un lote rompe la cadena de hashes")
    void tamperingIsDetected() throws Exception {
        String body = createLot("100").andReturn().getResponse().getContentAsString();
        String lotId = extract(body, "id");
        String code = extract(body, "code");
        transfer(lotId, "BUYER", "Comprador", null, "Nuquí").andExpect(status().isCreated());

        jdbc.update("UPDATE lots SET weight_kg = 300 WHERE id = ?", lotId);
        entityManager.clear();

        mockMvc.perform(get("/api/v1/trazabilidad/" + code))
                .andExpect(jsonPath("$.integrityOk").value(false));
    }

    @Test
    @DisplayName("TC-HU09-06 · El lote ajeno no existe para otro pescador; el código inexistente da 404")
    void lotsArePrivate() throws Exception {
        String lotId = extract(createLot("10").andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(get("/api/v1/lotes/" + lotId).header("Authorization", otherToken))
                .andExpect(status().isNotFound());
        transfer(lotId, "BUYER", "Ajeno", null, "Quibdó", otherToken).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/trazabilidad/LOT-NOEXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOT_NOT_FOUND"));
    }

    private ResultActions createLot(String weight) throws Exception {
        return mockMvc.perform(post("/api/v1/lotes").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"catchId":"%s","weightKg":%s}""".formatted(catchId, weight)));
    }

    private ResultActions transfer(String lotId, String type, String name, String taxId, String place)
            throws Exception {
        return transfer(lotId, type, name, taxId, place, fisherToken);
    }

    private ResultActions transfer(String lotId, String type, String name, String taxId, String place, String token)
            throws Exception {
        return mockMvc.perform(post("/api/v1/lotes/" + lotId + "/transferencias").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"partyType":"%s","partyName":"%s",%s"location":"%s"}"""
                        .formatted(type, name, taxId == null ? "" : "\"partyTaxId\":\"" + taxId + "\",", place)));
    }
}
