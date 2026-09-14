package com.sigepes.backend.permits;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;

/** RF-08 · Ciclo de vida del permiso: solicitud, aprobación, rechazo y suspensión. */
class PermitLifecycleTest extends ApiTestSupport {

    private UserEntity fisher;
    private String fisherToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        fisher = givenUser("7070707070", "solicitante@sigepes.gov.co", Role.FISHER);
        givenUser("8080808080", "revisor@sigepes.gov.co", Role.INSPECTOR);
        fisherToken = bearerFor("solicitante@sigepes.gov.co");
        inspectorToken = bearerFor("revisor@sigepes.gov.co");
    }

    @Test
    @DisplayName("Una solicitud nace en estado PENDING con constancia verificable")
    void requestsPermit() throws Exception {
        mockMvc.perform(post("/api/v1/permisos").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"Pesca artesanal"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.ownerId").value(fisher.getId()))
                .andExpect(jsonPath("$.holder").value(fisher.getFullName()))
                .andExpect(jsonPath("$.verificationCode").isNotEmpty());
    }

    @Test
    @DisplayName("Un pescador no puede auto-aprobarse el permiso al solicitarlo")
    void cannotSelfApprove() throws Exception {
        mockMvc.perform(post("/api/v1/permisos").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"Pesca artesanal","status":"APPROVED"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("El inspector aprueba un permiso pendiente")
    void inspectorApproves() throws Exception {
        String id = givenPermit(fisher, "PES-LC-1", PermitStatus.PENDING, LocalDate.now(),
                LocalDate.now().plusYears(1)).getId();

        mockMvc.perform(patch("/api/v1/permisos/" + id + "/evaluar").header("Authorization", inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"APPROVED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("El rechazo exige motivo")
    void rejectionRequiresReason() throws Exception {
        String id = givenPermit(fisher, "PES-LC-2", PermitStatus.PENDING, LocalDate.now(),
                LocalDate.now().plusYears(1)).getId();

        mockMvc.perform(patch("/api/v1/permisos/" + id + "/evaluar").header("Authorization", inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"REJECTED"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PERMIT_REASON_REQUIRED"));

        mockMvc.perform(patch("/api/v1/permisos/" + id + "/evaluar").header("Authorization", inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"REJECTED","reason":"Documentación incompleta"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Documentación incompleta"));
    }

    @Test
    @DisplayName("La suspensión exige motivo y queda registrada")
    void suspensionRequiresReason() throws Exception {
        String id = givenPermit(fisher, "PES-LC-3", PermitStatus.APPROVED, LocalDate.now().minusMonths(1),
                LocalDate.now().plusYears(1)).getId();

        mockMvc.perform(patch("/api/v1/permisos/" + id + "/evaluar").header("Authorization", inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"SUSPENDED","reason":"Infracción reiterada"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("Un pescador no puede evaluar permisos")
    void fisherCannotEvaluate() throws Exception {
        String id = givenPermit(fisher, "PES-LC-4", PermitStatus.PENDING, LocalDate.now(),
                LocalDate.now().plusYears(1)).getId();

        mockMvc.perform(patch("/api/v1/permisos/" + id + "/evaluar").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"APPROVED"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Un permiso aprobado pero pasado de fecha se informa como vencido")
    void reportsExpired() throws Exception {
        givenPermit(fisher, "PES-LC-5", PermitStatus.APPROVED, LocalDate.now().minusYears(2),
                LocalDate.now().minusDays(1));

        mockMvc.perform(get("/api/v1/permisos").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("EXPIRED"));
    }

    @Test
    @DisplayName("Marca los permisos próximos a vencer")
    void flagsExpiringSoon() throws Exception {
        givenPermit(fisher, "PES-LC-6", PermitStatus.APPROVED, LocalDate.now().minusMonths(6),
                LocalDate.now().plusDays(10));

        mockMvc.perform(get("/api/v1/permisos").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expiringSoon").value(true))
                .andExpect(jsonPath("$[0].daysToExpire").value(10));
    }

    @Test
    @DisplayName("Verifica la constancia por número de permiso")
    void validatesByNumber() throws Exception {
        givenPermit(fisher, "PES-LC-7", PermitStatus.APPROVED, LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1));

        mockMvc.perform(get("/api/v1/permisos/validar/PES-LC-7").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("PES-LC-7"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Un pescador solo ve sus propios permisos")
    void listsOnlyOwnPermits() throws Exception {
        givenPermit(fisher, "PES-LC-8", PermitStatus.APPROVED, LocalDate.now(), LocalDate.now().plusYears(1));
        UserEntity other = givenUser("9090909090", "otro.titular@sigepes.gov.co", Role.FISHER);
        givenPermit(other, "PES-LC-9", PermitStatus.APPROVED, LocalDate.now(), LocalDate.now().plusYears(1));

        mockMvc.perform(get("/api/v1/permisos").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].number").value("PES-LC-8"));
    }
}
