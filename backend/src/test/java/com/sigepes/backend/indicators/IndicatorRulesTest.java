package com.sigepes.backend.indicators;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;

/** HU-12 · RF-12 / RN-24 · Tablero de indicadores de sostenibilidad. */
class IndicatorRulesTest extends ApiTestSupport {

    @Autowired
    private IndicatorService indicatorService;

    private SpeciesEntity snapper;
    private SpeciesEntity tuna;
    private String fisherToken;
    private String otherToken;
    private String analystToken;
    private VesselEntity vessel;

    @BeforeEach
    void setUp() throws Exception {
        indicatorService.evictAll();
        UserEntity fisher = givenUser("3131313131", "pescador.ind@sigepes.gov.co", Role.FISHER);
        UserEntity other = givenUser("3131313132", "otro.ind@sigepes.gov.co", Role.FISHER);
        givenUser("3131313133", "analista.ind@sigepes.gov.co", Role.ANALYST);
        vessel = givenVessel(fisher, "DIMAR-IND-001", VesselStatus.ACTIVE, givenGear("Línea ind", false));
        VesselEntity otherVessel = givenVessel(other, "DIMAR-IND-002", VesselStatus.ACTIVE, givenGear("Línea ind 2", false));
        snapper = givenSpecies("Pargo indicador", new BigDecimal("35.00"));
        tuna = givenSpecies("Atún indicador", new BigDecimal("50.00"));
        givenPermit(fisher, "PES-IND-1", PermitStatus.APPROVED, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6));
        givenPermit(other, "PES-IND-2", PermitStatus.APPROVED, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6));
        fisherToken = bearerFor("pescador.ind@sigepes.gov.co");
        otherToken = bearerFor("otro.ind@sigepes.gov.co");
        analystToken = bearerFor("analista.ind@sigepes.gov.co");

        closedTrip(fisherToken, vessel.getId(), "Caribe", snapper, "100");
        closedTrip(otherToken, otherVessel.getId(), "Pacífico Norte", tuna, "250");
        // Faena abierta: su captura no cuenta (RN-24) pero se informa como excluida (AC-12.3).
        String open = apiStartedTripId(fisherToken, vessel.getId(), "Caribe");
        apiAddCatch(fisherToken, open, snapper.getId(), "40", "999").andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-HU12-01 · Solo cuentan las faenas cerradas; las abiertas se rotulan como excluidas")
    void countsOnlyValidatedData() throws Exception {
        mockMvc.perform(get("/api/v1/indicadores").header("Authorization", analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.landedKg").value(350.0))
                .andExpect(jsonPath("$.totals.closedTrips").value(2))
                .andExpect(jsonPath("$.totals.excludedOpenTrips").value(1))
                .andExpect(jsonPath("$.landingsBySpecies[0].speciesName").value("Atún indicador"))
                .andExpect(jsonPath("$.landingsBySpecies[0].kg").value(250.0))
                .andExpect(jsonPath("$.landingsByRegion.length()").value(2));
    }

    @Test
    @DisplayName("TC-HU12-02 · Filtros por región y especie (AC-12.2)")
    void filtersByRegionAndSpecies() throws Exception {
        mockMvc.perform(get("/api/v1/indicadores").param("region", "CARIBE").header("Authorization", analystToken))
                .andExpect(jsonPath("$.totals.landedKg").value(100.0))
                .andExpect(jsonPath("$.landingsBySpecies.length()").value(1))
                .andExpect(jsonPath("$.filters.region").value("CARIBE"));
        mockMvc.perform(get("/api/v1/indicadores").param("especieId", tuna.getId()).header("Authorization", analystToken))
                .andExpect(jsonPath("$.totals.landedKg").value(250.0));
        mockMvc.perform(get("/api/v1/indicadores")
                        .param("desde", LocalDate.now().plusDays(1).toString())
                        .param("hasta", LocalDate.now().plusDays(5).toString())
                        .header("Authorization", analystToken))
                .andExpect(jsonPath("$.totals.landedKg").value(0));
    }

    @Test
    @DisplayName("TC-HU12-03 · AC-12.6: un pescador recibe 403 en el tablero y solo ve sus propias cifras")
    void fishersOnlySeeTheirOwnFigures() throws Exception {
        mockMvc.perform(get("/api/v1/indicadores").header("Authorization", fisherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/indicadores/mios").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.landedKg").value(100.0))
                .andExpect(jsonPath("$.closedTrips").value(1))
                .andExpect(jsonPath("$.openTrips").value(1))
                .andExpect(jsonPath("$.landingsBySpecies.length()").value(1));
    }

    @Test
    @DisplayName("TC-HU12-04 · AC-12.5: los agregados se reutilizan dentro de la ventana de 15 minutos")
    void aggregatesAreCached() throws Exception {
        String first = mockMvc.perform(get("/api/v1/indicadores").header("Authorization", analystToken))
                .andReturn().getResponse().getContentAsString();
        closedTrip(fisherToken, vessel.getId(), "Caribe", snapper, "10");
        String second = mockMvc.perform(get("/api/v1/indicadores").header("Authorization", analystToken))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(extract(second, "generatedAt")).isEqualTo(extract(first, "generatedAt"));

        indicatorService.evictAll();
        mockMvc.perform(get("/api/v1/indicadores").header("Authorization", analystToken))
                .andExpect(jsonPath("$.totals.landedKg").value(360.0));
    }

    @Test
    @DisplayName("Periodo inválido: la fecha inicial no puede ser posterior a la final")
    void rejectsInvertedRange() throws Exception {
        mockMvc.perform(get("/api/v1/indicadores")
                        .param("desde", LocalDate.now().toString())
                        .param("hasta", LocalDate.now().minusDays(3).toString())
                        .header("Authorization", analystToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private void closedTrip(String token, String vesselId, String zone, SpeciesEntity species, String kg)
            throws Exception {
        String trip = apiStartedTripId(token, vesselId, zone);
        apiAddCatch(token, trip, species.getId(), "60", kg).andExpect(status().isOk());
        apiCloseTrip(token, trip).andExpect(status().isOk());
    }
}
