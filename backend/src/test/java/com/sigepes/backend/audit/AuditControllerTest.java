package com.sigepes.backend.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;

/** RF-20 · Tipo de acción, filtros de la vista de auditoría y alerta por fuerza bruta. */
class AuditControllerTest extends ApiTestSupport {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private UserEntity fisher;
    private String fisherToken;
    private String adminToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        fisher = givenUser("5050505050", "audctrl.pescador@sigepes.gov.co", Role.FISHER);
        givenUser("5151515151", "audctrl.admin@sigepes.gov.co", Role.ADMIN);
        givenUser("5252525252", "audctrl.inspector@sigepes.gov.co", Role.INSPECTOR);
        fisherToken = bearerFor("audctrl.pescador@sigepes.gov.co");
        adminToken = bearerFor("audctrl.admin@sigepes.gov.co");
        inspectorToken = bearerFor("audctrl.inspector@sigepes.gov.co");
    }

    @Test
    @DisplayName("AC-20.2: clasifica correctamente CREATE, UPDATE y SECURITY")
    void classifiesActionType() throws Exception {
        String vesselId = extract(mockMvc.perform(post("/api/v1/embarcaciones").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Audit Test","registration":"DIMAR-AUD-CTRL-1","port":"Tumaco",
                        "hullType":"Fibra de vidrio","enginePower":75,"capacityTons":3.5}"""))
                .andReturn().getResponse().getContentAsString(), "id");

        AuditLogEntity created = auditLogRepository.findAll().stream()
                .filter(log -> "VESSEL_CREATED".equals(log.getAction()) && vesselId.equals(log.getEntityId()))
                .findFirst().orElseThrow();
        assertEquals(AuditActionType.CREATE, created.getActionType());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"audctrl.pescador@sigepes.gov.co","password":"ClaveIncorrecta#9"}"""))
                .andExpect(status().isUnauthorized());
        AuditLogEntity failed = auditLogRepository.findAll().stream()
                .filter(log -> "LOGIN_FAILED".equals(log.getAction()) && fisher.getId().equals(log.getActorId()))
                .findFirst().orElseThrow();
        assertEquals(AuditActionType.SECURITY, failed.getActionType());
    }

    @Test
    @DisplayName("AC-20.4: filtra por tipo de acción, usuario y rango de fechas")
    void filtersAuditLog() throws Exception {
        mockMvc.perform(post("/api/v1/embarcaciones").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Audit Filter","registration":"DIMAR-AUD-CTRL-2","port":"Tumaco",
                        "hullType":"Fibra de vidrio","enginePower":75,"capacityTons":3.5}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/auditoria").header("Authorization", adminToken)
                .param("actionType", "CREATE")
                .param("actorId", fisher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action=='VESSEL_CREATED')]").exists())
                .andExpect(jsonPath("$[?(@.actionType!='CREATE')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.actorId!='" + fisher.getId() + "')]").doesNotExist());

        // Un rango de fechas que empieza mañana no debe incluir el evento recién creado.
        mockMvc.perform(get("/api/v1/auditoria").header("Authorization", adminToken)
                .param("actorId", fisher.getId())
                .param("from", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action=='VESSEL_CREATED')]").doesNotExist());
    }

    @Test
    @DisplayName("AC-20.5: detecta fallos de login repetidos desde el mismo origen y solo el ADMIN la ve")
    void detectsSuspiciousIp() throws Exception {
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"audctrl.pescador@sigepes.gov.co","password":"Incorrecta#%d"}""".formatted(i)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(get("/api/v1/resumen").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("actividad sospechosa"))));

        mockMvc.perform(get("/api/v1/notificaciones").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=~/security-.*/)]").exists());

        // Un INSPECTOR (autoridad, pero no ADMIN) no debe ver el incidente de seguridad.
        mockMvc.perform(get("/api/v1/notificaciones").header("Authorization", inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=~/security-.*/)]").doesNotExist());

        assertTrue(auditLogRepository.findSuspiciousLoginIps(
                        java.time.Instant.now().minusSeconds(900), 5).size() > 0,
                "Se esperaba al menos una IP marcada como sospechosa");
    }
}
