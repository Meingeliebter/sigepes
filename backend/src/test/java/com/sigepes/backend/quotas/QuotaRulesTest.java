package com.sigepes.backend.quotas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.sigepes.backend.common.Ids;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;

/** HU-07 · RF-07 / RN-14 · Control de cuotas de pesca por especie. */
@RecordApplicationEvents
class QuotaRulesTest extends ApiTestSupport {

    private static final String ZONE = "Caribe";

    @Autowired
    private QuotaRepository quotaRepository;

    @Autowired
    private ApplicationEvents events;

    private UserEntity fisher;
    private VesselEntity vessel;
    private SpeciesEntity snapper;
    private String fisherToken;

    @BeforeEach
    void setUp() throws Exception {
        fisher = givenUser("3030303030", "pescador.cuota@sigepes.gov.co", Role.FISHER);
        givenUser("4040404040", "analista.cuota@sigepes.gov.co", Role.ANALYST);
        vessel = givenVessel(fisher, "DIMAR-CUO-001", VesselStatus.ACTIVE, givenGear("Línea cuota", false));
        snapper = givenSpecies("Pargo cuota", new BigDecimal("35.00"));
        givenPermit(fisher, "PES-CUO-1", PermitStatus.APPROVED, LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(6));
        fisherToken = bearerFor("pescador.cuota@sigepes.gov.co");
    }

    @Test
    @DisplayName("TC-HU07-01 · El analista crea una cuota con su saldo completo")
    void analystCreatesQuota() throws Exception {
        String analyst = bearerFor("analista.cuota@sigepes.gov.co");
        mockMvc.perform(post("/api/v1/cuotas").header("Authorization", analyst)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","zone":"%s","from":"%s","to":"%s","totalKg":2000}"""
                        .formatted(snapper.getId(), ZONE, LocalDate.now().minusDays(1),
                                LocalDate.now().plusMonths(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingKg").value(2000))
                .andExpect(jsonPath("$.consumedPercent").value(0));
    }

    @Test
    @DisplayName("AC-07.1 · Un pescador no puede crear cuotas")
    void fisherCannotCreateQuota() throws Exception {
        mockMvc.perform(post("/api/v1/cuotas").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","zone":"%s","from":"%s","to":"%s","totalKg":10}"""
                        .formatted(snapper.getId(), ZONE, LocalDate.now(), LocalDate.now().plusDays(5))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-HU07-02 · Al cerrar la faena la captura se descuenta de la cuota")
    void closingTripDiscountsQuota() throws Exception {
        QuotaEntity quota = givenQuota("2000.00", "0.00");
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), ZONE);
        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "150").andExpect(status().isOk());
        apiCloseTrip(fisherToken, tripId).andExpect(status().isOk());

        assertThat(quotaRepository.findById(quota.getId()).orElseThrow().remainingKg())
                .isEqualByComparingTo("1850.00");
        mockMvc.perform(get("/api/v1/cuotas/saldo").header("Authorization", fisherToken)
                .param("speciesId", snapper.getId()).param("zone", ZONE).param("vesselId", vessel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingKg").value(1850.0));
    }

    @Test
    @DisplayName("TC-HU07-03 · RN-14: rechaza la captura que excede el saldo disponible")
    void rejectsCatchOverQuota() throws Exception {
        givenQuota("100.00", "50.00");
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), ZONE);

        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "80")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUOTA_EXHAUSTED"))
                .andExpect(jsonPath("$.message").value(containsString("Cuota agotada")));
    }

    @Test
    @DisplayName("TC-HU07-03 · Suma las capturas de la misma faena antes de validar el saldo")
    void sumsPendingCatchesOfSameTrip() throws Exception {
        givenQuota("100.00", "0.00");
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), ZONE);
        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "70").andExpect(status().isOk());

        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "40")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUOTA_EXHAUSTED"));
    }

    @Test
    @DisplayName("TC-HU07-05 · Si la cuota se agotó antes del cierre, el cierre se rechaza y no se descuenta nada")
    void closeRejectedWhenQuotaExhaustedMeanwhile() throws Exception {
        QuotaEntity quota = givenQuota("100.00", "0.00");
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), ZONE);
        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "60").andExpect(status().isOk());

        QuotaEntity meanwhile = quotaRepository.findById(quota.getId()).orElseThrow();
        meanwhile.consume(new BigDecimal("80.00"));
        quotaRepository.saveAndFlush(meanwhile);

        apiCloseTrip(fisherToken, tripId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUOTA_EXHAUSTED"));
        assertThat(quotaRepository.findById(quota.getId()).orElseThrow().getConsumedKg())
                .isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("AC-07.4 · Publica el umbral del 80 % una sola vez, dirigido al dueño de la faena")
    void publishesThresholdOnce() throws Exception {
        givenQuota("100.00", "70.00");
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), ZONE);
        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "12").andExpect(status().isOk());
        apiCloseTrip(fisherToken, tripId).andExpect(status().isOk());

        assertThat(events.stream(QuotaThresholdReached.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.threshold()).isEqualTo(80);
                    assertThat(event.vesselOwnerId()).isEqualTo(fisher.getId());
                });
    }

    @Test
    @DisplayName("Una especie sin cuota no se ve afectada")
    void speciesWithoutQuotaIsNotAffected() throws Exception {
        String tripId = apiStartedTripId(fisherToken, vessel.getId(), ZONE);
        apiAddCatch(fisherToken, tripId, snapper.getId(), "40", "999").andExpect(status().isOk());
        apiCloseTrip(fisherToken, tripId).andExpect(status().isOk());
    }

    private QuotaEntity givenQuota(String total, String consumed) {
        QuotaEntity quota = new QuotaEntity(Ids.generate("cuo"), snapper, ZONE, LocalDate.now().minusDays(10),
                LocalDate.now().plusMonths(1), new BigDecimal(total));
        quota.consume(new BigDecimal(consumed));
        return quotaRepository.save(quota);
    }
}
