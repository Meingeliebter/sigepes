package com.sigepes.backend.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselStatus;

/** RF-20 · Auditoría de operaciones sensibles y autorización por rol. */
class AuditAndAuthorizationTest extends ApiTestSupport {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private UserEntity fisher;
    private String fisherToken;
    private String adminToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        fisher = givenUser("1112223330", "aud.pescador@sigepes.gov.co", Role.FISHER);
        givenUser("1112223331", "aud.admin@sigepes.gov.co", Role.ADMIN);
        givenUser("1112223332", "aud.inspector@sigepes.gov.co", Role.INSPECTOR);
        fisherToken = bearerFor("aud.pescador@sigepes.gov.co");
        adminToken = bearerFor("aud.admin@sigepes.gov.co");
        inspectorToken = bearerFor("aud.inspector@sigepes.gov.co");
    }

    // ------------------------------------------------------------------
    // Auditoría
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El inicio de sesión queda auditado")
    void auditsLogin() {
        assertTrue(hasAction("LOGIN"), "Se esperaba un evento LOGIN en la bitácora");
    }

    @Test
    @DisplayName("El intento fallido de inicio de sesión queda auditado")
    void auditsFailedLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"aud.pescador@sigepes.gov.co","password":"ClaveIncorrecta#9"}"""))
                .andExpect(status().isUnauthorized());

        assertTrue(hasAction("LOGIN_FAILED"), "Se esperaba un evento LOGIN_FAILED en la bitácora");
    }

    @Test
    @DisplayName("El cierre de una faena queda auditado")
    void auditsTripClosure() throws Exception {
        givenPermit(fisher, "PES-AUD-1", PermitStatus.APPROVED, LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1));
        var vessel = givenVessel(fisher, "DIMAR-AUD-1", VesselStatus.ACTIVE);

        String tripResponse = mockMvc.perform(post("/api/v1/faenas").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"Pacífico Norte"}""".formatted(vessel.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String tripId = extract(tripResponse, "id");

        mockMvc.perform(post("/api/v1/faenas/" + tripId + "/cerrar").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nullDeclaration":true}"""))
                .andExpect(status().isOk());

        assertTrue(hasAction("TRIP_STARTED"));
        assertTrue(hasAction("TRIP_CLOSED"));
    }

    @Test
    @DisplayName("La aprobación de un permiso queda auditada con estado anterior y posterior")
    void auditsPermitApproval() throws Exception {
        String id = givenPermit(fisher, "PES-AUD-2", PermitStatus.PENDING, LocalDate.now(),
                LocalDate.now().plusYears(1)).getId();

        mockMvc.perform(patch("/api/v1/permisos/" + id + "/evaluar").header("Authorization", inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"APPROVED"}"""))
                .andExpect(status().isOk());

        AuditLogEntity entry = auditLogRepository.findAll().stream()
                .filter(log -> "PERMIT_APPROVED".equals(log.getAction()))
                .findFirst().orElseThrow();
        assertTrue(entry.getBeforeState().contains("PENDING"));
        assertTrue(entry.getAfterState().contains("APPROVED"));
        assertTrue(entry.getActorLabel().contains("INSPECTOR"));
    }

    @Test
    @DisplayName("La corrección de una faena cerrada queda auditada")
    void auditsTripCorrection() throws Exception {
        givenPermit(fisher, "PES-AUD-3", PermitStatus.APPROVED, LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1));
        var vessel = givenVessel(fisher, "DIMAR-AUD-2", VesselStatus.ACTIVE);
        var species = givenSpecies("Corvina aud", new BigDecimal("30.00"));

        String tripId = extract(mockMvc.perform(post("/api/v1/faenas").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"Caribe"}""".formatted(vessel.getId())))
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(post("/api/v1/faenas/" + tripId + "/capturas").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","weightKg":50,"averageSizeCm":35}""".formatted(species.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/faenas/" + tripId + "/cerrar").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        // Cerrada: el pescador ya no puede corregirla, la autoridad sí.
        mockMvc.perform(patch("/api/v1/faenas/" + tripId).header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"notes":"intento"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/faenas/" + tripId).header("Authorization", inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"notes":"Corrección por inspección en muelle"}"""))
                .andExpect(status().isOk());

        assertTrue(hasAction("TRIP_CORRECTED"));
    }

    @Test
    @DisplayName("La bitácora no expone ningún endpoint de escritura")
    void auditIsAppendOnly() throws Exception {
        mockMvc.perform(post("/api/v1/auditoria").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/auditoria").header("Authorization", adminToken))
                .andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------
    // Autorización por rol
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un pescador no puede leer la auditoría")
    void fisherCannotReadAudit() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria").header("Authorization", fisherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("El inspector y el administrador sí pueden leer la auditoría")
    void authorityReadsAudit() throws Exception {
        for (String token : List.of(adminToken, inspectorToken)) {
            mockMvc.perform(get("/api/v1/auditoria").header("Authorization", token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Un pescador no puede listar ni crear usuarios")
    void fisherCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", fisherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nationalId":"1212121212","fullName":"Intruso Demo","email":"intruso@sigepes.gov.co",
                         "role":"ADMIN","password":"Pesca#2026"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un pescador no puede modificar el catálogo de especies ni las vedas")
    void fisherCannotManageCatalogs() throws Exception {
        mockMvc.perform(post("/api/v1/especies").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"commonName":"Falsa","scientificName":"Falsus falsus","minSizeCm":10}"""))
                .andExpect(status().isForbidden());

        var species = givenSpecies("Especie protegida", new BigDecimal("20.00"));
        mockMvc.perform(post("/api/v1/vedas").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","zone":"Caribe","from":"2026-01-01","to":"2026-02-01"}"""
                        .formatted(species.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El administrador gestiona usuarios y la suspensión surte efecto de inmediato")
    void adminManagesUsers() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(patch("/api/v1/usuarios/" + fisher.getId()).header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"SUSPENDED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // El token del pescador suspendido deja de servir sin esperar a que caduque.
        mockMvc.perform(get("/api/v1/faenas").header("Authorization", fisherToken))
                .andExpect(status().isUnauthorized());
    }

    private boolean hasAction(String action) {
        return auditLogRepository.findAll().stream().anyMatch(log -> action.equals(log.getAction()));
    }
}
