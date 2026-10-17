package com.sigepes.backend.documents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.sigepes.backend.audit.AuditLogRepository;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;

/** HU-15 · RF-15 / RN-30 · Gestión documental versionada. */
class DocumentRulesTest extends ApiTestSupport {

    private static final byte[] PDF_V1 = "%PDF-1.7\n1 0 obj << /Title (Certificado) >> endobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] PDF_V2 = "%PDF-1.7\n1 0 obj << /Title (Certificado 2027) >> endobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired
    private AuditLogRepository auditRepository;

    private VesselEntity vessel;
    private String fisherToken;
    private String otherToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        UserEntity fisher = givenUser("4141414141", "pescador.doc@sigepes.gov.co", Role.FISHER);
        givenUser("4141414142", "otro.doc@sigepes.gov.co", Role.FISHER);
        givenUser("4141414143", "inspector.doc@sigepes.gov.co", Role.INSPECTOR);
        vessel = givenVessel(fisher, "DIMAR-DOC-001", VesselStatus.ACTIVE, givenGear("Línea doc", false));
        fisherToken = bearerFor("pescador.doc@sigepes.gov.co");
        otherToken = bearerFor("otro.doc@sigepes.gov.co");
        inspectorToken = bearerFor("inspector.doc@sigepes.gov.co");
    }

    @Test
    @DisplayName("TC-HU15-01 · Carga un certificado vinculado a su embarcación, con vencimiento")
    void uploadsDocument() throws Exception {
        upload(fisherToken, LocalDate.now().plusYears(1), PDF_V1, "certificado.pdf")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andExpect(jsonPath("$.expiryStatus").value("VALID"))
                .andExpect(jsonPath("$.entityLabel").value("Embarcación Embarcación DIMAR-DOC-001 (DIMAR-DOC-001)"))
                .andExpect(jsonPath("$.versions[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.versions[0].sha256").isNotEmpty());
    }

    @Test
    @DisplayName("TC-HU15-02 · RN-30: una carga nueva es otra versión y la anterior se conserva")
    void versionsArePreserved() throws Exception {
        String id = extract(upload(fisherToken, LocalDate.now().plusDays(20), PDF_V1, "certificado.pdf")
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(multipart("/api/v1/documentos/" + id + "/versiones").header("Authorization", fisherToken)
                        .file(json("""
                                {"expiresOn":"%s","note":"Renovación 2027"}""".formatted(LocalDate.now().plusYears(1))))
                        .file(new MockMultipartFile("file", "certificado-2027.pdf", "application/pdf", PDF_V2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentVersion").value(2))
                .andExpect(jsonPath("$.expiryStatus").value("VALID"))
                .andExpect(jsonPath("$.versions.length()").value(2))
                .andExpect(jsonPath("$.versions[1].version").value(1))
                .andExpect(jsonPath("$.versions[1].originalFilename").value("certificado.pdf"));

        // La versión 1 se sigue pudiendo descargar intacta.
        String url = extract(mockMvc.perform(post("/api/v1/documentos/" + id + "/versiones/1/enlace")
                .header("Authorization", fisherToken)).andReturn().getResponse().getContentAsString(), "url");
        mockMvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(PDF_V1));
    }

    @Test
    @DisplayName("TC-HU15-03 · AC-15.5: un ejecutable renombrado a .pdf se rechaza y el intento queda auditado")
    void rejectsFakePdf() throws Exception {
        upload(fisherToken, null, "MZ\u0090\u0000 este no es un PDF".getBytes(StandardCharsets.ISO_8859_1), "factura.pdf")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DOCUMENT_FILE"));

        assertThat(auditRepository.findAll()).anyMatch(entry -> "DOCUMENT_REJECTED".equals(entry.getAction())
                && entry.getDetail().contains("factura.pdf"));
    }

    @Test
    @DisplayName("TC-HU15-03 · AC-15.1: más de 5 MB se rechaza")
    void rejectsTooLarge() throws Exception {
        byte[] big = new byte[(int) DocumentService.MAX_BYTES + 1];
        System.arraycopy(PDF_V1, 0, big, 0, PDF_V1.length);
        upload(fisherToken, null, big, "grande.pdf")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DOCUMENT_FILE"));
    }

    @Test
    @DisplayName("TC-HU15-04 · Enlace firmado: titular e inspector sí, otro usuario no; alterado o vencido no sirve")
    void signedLinks() throws Exception {
        String id = extract(upload(fisherToken, null, PDF_V1, "certificado.pdf")
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(post("/api/v1/documentos/" + id + "/versiones/1/enlace").header("Authorization", otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/documentos/" + id).header("Authorization", otherToken))
                .andExpect(status().isNotFound());

        String url = extract(mockMvc.perform(post("/api/v1/documentos/" + id + "/versiones/1/enlace")
                .header("Authorization", inspectorToken)).andReturn().getResponse().getContentAsString(), "url");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(PDF_V1));
        assertThat(auditRepository.findAll()).anyMatch(entry -> "DOCUMENT_DOWNLOADED".equals(entry.getAction())
                && id.equals(entry.getEntityId()));

        String tampered = url.substring(0, url.length() - 3) + (url.endsWith("AAA") ? "BBB" : "AAA");
        mockMvc.perform(get(tampered))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("DOCUMENT_LINK_EXPIRED"));
    }

    @Test
    @DisplayName("TC-HU15-05 · No se puede vincular a registros ajenos; las vedas son solo de la autoridad")
    void linksOnlyToOwnRecords() throws Exception {
        upload(otherToken, null, PDF_V1, "ajeno.pdf").andExpect(status().isForbidden());

        String closureId = givenClosure(givenSpecies("Robalo doc", new java.math.BigDecimal("40")), "Caribe",
                LocalDate.now(), LocalDate.now().plusDays(30)).getId();
        mockMvc.perform(multipart("/api/v1/documentos").header("Authorization", fisherToken)
                        .file(json("""
                                {"docType":"RESOLUTION","title":"Resolución","entityType":"CLOSURE","entityId":"%s"}"""
                                .formatted(closureId)))
                        .file(new MockMultipartFile("file", "res.pdf", "application/pdf", PDF_V1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-HU15-06 · AC-15.3: el vencimiento se refleja en el estado, en los filtros y en las alertas")
    void expiryFeedsStatusAndAlerts() throws Exception {
        upload(fisherToken, LocalDate.now().plusDays(10), PDF_V1, "certificado.pdf")
                .andExpect(jsonPath("$.expiryStatus").value("EXPIRING"));

        mockMvc.perform(get("/api/v1/documentos").param("estado", "EXPIRING").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/documentos").param("estado", "EXPIRED").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.length()").value(0));

        alertSweeper.sweep();
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", fisherToken))
                .andExpect(jsonPath("$.alerts[?(@.type=='DOCUMENT')].severity").value("WARNING"));
    }

    private ResultActions upload(String token, LocalDate expiresOn, byte[] bytes, String filename) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/documentos");
        request.file(json("""
                {"docType":"VESSEL_CERTIFICATE","title":"Certificado de navegabilidad","entityType":"VESSEL",
                 "entityId":"%s"%s}""".formatted(vessel.getId(),
                expiresOn == null ? "" : ",\"expiresOn\":\"" + expiresOn + "\"")));
        request.file(new MockMultipartFile("file", filename, "application/pdf", bytes));
        return mockMvc.perform(request.header("Authorization", token));
    }

    private static MockMultipartFile json(String body) {
        return new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, body.getBytes(StandardCharsets.UTF_8));
    }
}
