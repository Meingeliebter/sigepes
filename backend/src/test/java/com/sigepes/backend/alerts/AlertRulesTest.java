package com.sigepes.backend.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.sigepes.backend.geo.RestrictedZoneEntered;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.quotas.QuotaThresholdReached;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.common.AppClock;

/** HU-11 · RF-11 / RN-22 · Alertas y notificaciones de cumplimiento. */
class AlertRulesTest extends ApiTestSupport {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    private UserEntity fisher;
    private UserEntity shipowner;
    private String fisherToken;
    private String shipownerToken;
    private String inspectorToken;
    private String analystToken;

    @BeforeEach
    void setUp() throws Exception {
        fisher = givenUser("1111222233", "pescador.ale@sigepes.gov.co", Role.FISHER);
        shipowner = givenUser("1111222244", "armador.ale@sigepes.gov.co", Role.SHIPOWNER);
        givenUser("1111222255", "inspector.ale@sigepes.gov.co", Role.INSPECTOR);
        givenUser("1111222266", "analista.ale@sigepes.gov.co", Role.ANALYST);
        fisherToken = bearerFor("pescador.ale@sigepes.gov.co");
        shipownerToken = bearerFor("armador.ale@sigepes.gov.co");
        inspectorToken = bearerFor("inspector.ale@sigepes.gov.co");
        analystToken = bearerFor("analista.ale@sigepes.gov.co");
    }

    @Test
    @DisplayName("TC-HU11-01 · Cuota al 95 %: crítica para analistas y el armador de la embarcación, nadie más")
    void quotaThresholdReachesAnalystsAndOwner() throws Exception {
        publisher.publishEvent(new QuotaThresholdReached("cuo-test-1", "Pargo rojo", "Caribe", shipowner.getId(), 95,
                "96.0", "80.0"));

        mockMvc.perform(get("/api/v1/alertas").header("Authorization", analystToken))
                .andExpect(jsonPath("$.alerts[0].type").value("QUOTA"))
                .andExpect(jsonPath("$.alerts[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.alerts[0].link").value("/app/quotas"))
                .andExpect(jsonPath("$.unread").value(1));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", shipownerToken))
                .andExpect(jsonPath("$.alerts.length()").value(1));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.alerts.length()").value(0));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", inspectorToken))
                .andExpect(jsonPath("$.alerts.length()").value(0));
    }

    @Test
    @DisplayName("TC-HU11-02 · Ingreso a zona restringida: crítica para el pescador y la autoridad")
    void zoneEntryReachesFisherAndAuthority() throws Exception {
        publisher.publishEvent(new RestrictedZoneEntered("zev-test-1", "fae-test-1", fisher.getId(),
                "Veda camarón", "CLOSURE_ZONE", "La Esperanza", 6.2, -77.5));

        mockMvc.perform(get("/api/v1/alertas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.alerts[0].type").value("ZONE"))
                .andExpect(jsonPath("$.alerts[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.alerts[0].archivable").value(false));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", inspectorToken))
                .andExpect(jsonPath("$.alerts[0].link").value("/app/zones"));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", analystToken))
                .andExpect(jsonPath("$.alerts.length()").value(0));
    }

    @Test
    @DisplayName("TC-HU11-03 · Denuncia nueva: aviso a inspección sin datos del denunciante")
    void newReportReachesInspectors() throws Exception {
        mockMvc.perform(multipart("/api/v1/denuncias").header("Authorization", fisherToken)
                .file(new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, """
                        {"reportType":"OTHER","description":"Pesca con dinamita frente a la playa","occurredOn":"%s",
                         "locationText":"Playa de Nuquí","anonymous":false}""".formatted(AppClock.today())
                        .getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/alertas").header("Authorization", inspectorToken))
                .andExpect(jsonPath("$.alerts[0].type").value("REPORT"))
                .andExpect(jsonPath("$.alerts[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.alerts[0].message",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Usuario"))));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.alerts.length()").value(0));
    }

    @Test
    @DisplayName("TC-HU11-04 · Permisos por vencer y vedas: la revisión periódica no duplica alertas")
    void sweepIsIdempotent() throws Exception {
        givenPermit(fisher, "PES-ALE-1", PermitStatus.APPROVED, AppClock.today().minusMonths(6),
                AppClock.today().plusDays(12));
        givenClosure(givenSpecies("Robalo alerta", new BigDecimal("40")), "Pacífico Norte",
                AppClock.today().minusDays(2), AppClock.today().plusDays(20));

        assertThat(alertSweeper.sweep()).isGreaterThan(0);
        assertThat(alertSweeper.sweep()).isZero();

        mockMvc.perform(get("/api/v1/alertas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.alerts[?(@.type=='PERMIT')].severity").value("WARNING"))
                .andExpect(jsonPath("$.alerts[?(@.type=='CLOSURE')]").exists());
        // Las vedas se avisan a quienes pescan, no a la autoridad.
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", inspectorToken))
                .andExpect(jsonPath("$.alerts[?(@.type=='CLOSURE')]").doesNotExist());
    }

    @Test
    @DisplayName("TC-HU11-05 · Las críticas no se archivan; las demás se marcan leídas y se archivan")
    void criticalAlertsArePersistent() throws Exception {
        alertService.notify(fisher.getId(), AlertType.ZONE, AlertSeverity.CRITICAL, "Zona", "Dentro de zona",
                "/app/trips", "test:critical");
        alertService.notify(fisher.getId(), AlertType.CLOSURE, AlertSeverity.INFO, "Veda", "Veda próxima",
                "/app/closures", "test:info");
        String critical = alertRepository.findAll().stream()
                .filter(alert -> alert.getSourceKey().equals("test:critical")).findFirst().orElseThrow().getId();
        String info = alertRepository.findAll().stream()
                .filter(alert -> alert.getSourceKey().equals("test:info")).findFirst().orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/alertas/" + critical).header("Authorization", fisherToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ALERT_CRITICAL_PERSISTENT"));

        mockMvc.perform(patch("/api/v1/alertas/" + info + "/leida").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.read").value(true));
        mockMvc.perform(get("/api/v1/alertas/no-leidas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.unread").value(1));

        mockMvc.perform(delete("/api/v1/alertas/" + info).header("Authorization", fisherToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/alertas/leidas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.unread").value(0));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.alerts.length()").value(1))
                .andExpect(jsonPath("$.alerts[0].id").value(critical));
    }

    @Test
    @DisplayName("TC-HU11-06 · Nadie puede leer ni tocar las alertas de otro usuario")
    void alertsArePrivate() throws Exception {
        alertService.notify(fisher.getId(), AlertType.PERMIT, AlertSeverity.INFO, "Permiso", "Vence pronto",
                "/app/permits", "test:private");
        String id = alertRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/v1/alertas/" + id + "/leida").header("Authorization", shipownerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/alertas/" + id).header("Authorization", inspectorToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/alertas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-HU11-07 · AC-11.5/11.6: la alerta queda guardada y el canal en vivo abre para el usuario")
    void alertIsPersistedAndStreamOpens() throws Exception {
        mockMvc.perform(get("/api/v1/alertas/stream").header("Authorization", fisherToken)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted());

        publisher.publishEvent(new RestrictedZoneEntered("zev-test-2", "fae-test-2", fisher.getId(),
                "AMP Utría", "MPA", "Don Pacho", 6.0, -77.35));

        assertThat(alertRepository.existsByRecipientIdAndSourceKey(fisher.getId(), "zone:zev-test-2")).isTrue();
        mockMvc.perform(get("/api/v1/alertas/stream")).andExpect(status().isUnauthorized());
    }
}
