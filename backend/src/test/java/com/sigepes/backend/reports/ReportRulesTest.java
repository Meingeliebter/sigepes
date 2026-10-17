package com.sigepes.backend.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.sigepes.backend.audit.AuditLogEntity;
import com.sigepes.backend.audit.AuditLogRepository;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.common.AppClock;

/** HU-10 · RF-10 / RN-20 · Denuncias de pesca ilegal. */
@RecordApplicationEvents
class ReportRulesTest extends ApiTestSupport {

    @Autowired
    private IllegalReportRepository reportRepository;

    @Autowired
    private ReportPhotoRepository photoRepository;

    @Autowired
    private AuditLogRepository auditRepository;

    @Autowired
    private PhotoSanitizer photoSanitizer;

    @Autowired
    private ApplicationEvents events;

    private String fisherToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        givenUser("7070707070", "pescador.den@sigepes.gov.co", Role.FISHER);
        givenUser("8080808080", "inspector.den@sigepes.gov.co", Role.INSPECTOR);
        fisherToken = bearerFor("pescador.den@sigepes.gov.co");
        inspectorToken = bearerFor("inspector.den@sigepes.gov.co");
    }

    @Test
    @DisplayName("TC-HU10-01 · Un ciudadano sin cuenta radica una denuncia y recibe código de seguimiento")
    void citizenWithoutAccountSubmits() throws Exception {
        String response = submit(null, body(false, null), jpegPhoto())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.anonymous").value(true))
                .andReturn().getResponse().getContentAsString();
        String code = extract(response, "trackingCode");
        assertThat(code).matches("DEN-[2-9A-HJKMNP-Z]{8}");

        mockMvc.perform(get("/api/v1/denuncias/seguimiento/" + code.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.reportType").value("CLOSURE_FISHING"))
                // La consulta pública no revela el contenido de la denuncia.
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist());

        assertThat(events.stream(ReportSubmitted.class)).hasSize(1);
    }

    @Test
    @DisplayName("TC-HU10-02 · Denuncia anónima con sesión iniciada: sin usuario, sin IP y sin actor en la auditoría")
    void anonymousReportStoresNoIdentity() throws Exception {
        String response = submit(fisherToken, body(true, "cli-anon-0001"), jpegPhoto())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        IllegalReportEntity report = reportRepository.findByTrackingCode(extract(response, "trackingCode"))
                .orElseThrow();

        assertThat(report.isAnonymous()).isTrue();
        assertThat(report.getReporter()).isNull();

        List<AuditLogEntity> entries = auditRepository.findAll().stream()
                .filter(entry -> report.getId().equals(entry.getEntityId()))
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getActorId()).isNull();
        assertThat(entries.get(0).getActorLabel()).isEqualTo("anónimo");
        assertThat(entries.get(0).getIpAddress()).isNull();

        // La denuncia anónima tampoco aparece en "mis denuncias": no hay vínculo que la relacione.
        mockMvc.perform(get("/api/v1/denuncias/mias").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-HU10-02 · La denuncia identificada queda vinculada a su autor")
    void identifiedReportIsLinked() throws Exception {
        submit(fisherToken, body(false, null)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.anonymous").value(false));

        mockMvc.perform(get("/api/v1/denuncias/mias").header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("TC-HU10-03 · Las fotos se guardan sin EXIF y se rechazan archivos que no son imágenes")
    void photosAreStrippedAndValidated() throws Exception {
        byte[] withExif = jpegWithExif();
        assertThat(new String(withExif, StandardCharsets.ISO_8859_1)).contains("Exif");

        byte[] clean = photoSanitizer.sanitize(new MockMultipartFile("photos", "foto.jpg", "image/jpeg", withExif))
                .content();
        assertThat(new String(clean, StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
        assertThat(PhotoSanitizer.detectFormat(clean)).isEqualTo("jpg");

        // Un ejecutable renombrado a .jpg se rechaza por su firma, no por la extensión.
        submit(null, body(false, null),
                new MockMultipartFile("photos", "foto.jpg", "image/jpeg", "MZ\u0090\u0000fake".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPORT_PHOTO"));
    }

    @Test
    @DisplayName("TC-HU10-03 · Máximo 3 fotos por denuncia")
    void rejectsMoreThanThreePhotos() throws Exception {
        submit(null, body(false, null), jpegPhoto(), jpegPhoto(), jpegPhoto(), jpegPhoto())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REPORT_PHOTOS"));
    }

    @Test
    @DisplayName("TC-HU10-04 · Flujo del inspector Recibida → En investigación → Cerrada, con nota obligatoria")
    void inspectorWorkflow() throws Exception {
        String code = extract(submit(null, body(false, null), jpegPhoto()).andReturn().getResponse()
                .getContentAsString(), "trackingCode");
        String id = reportRepository.findByTrackingCode(code).orElseThrow().getId();

        // No se puede cerrar sin investigar.
        changeStatus(id, "CLOSED_FOUNDED", "Se confirma la faena ilegal")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REPORT_TRANSITION"));
        // La nota es obligatoria.
        changeStatus(id, "INVESTIGATING", "").andExpect(status().isBadRequest());

        changeStatus(id, "INVESTIGATING", "Se asigna patrulla de la Armada")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"));
        changeStatus(id, "CLOSED_FOUNDED", "Se incautó la red y se abrió proceso sancionatorio")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED_FOUNDED"))
                .andExpect(jsonPath("$.resolutionNote").value("Se incautó la red y se abrió proceso sancionatorio"))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.photoIds.length()").value(1));

        mockMvc.perform(get("/api/v1/denuncias/seguimiento/" + code))
                .andExpect(jsonPath("$.status").value("CLOSED_FOUNDED"));
    }

    @Test
    @DisplayName("TC-HU10-05 · Solo INSPECTOR y ADMIN leen el contenido de las denuncias")
    void onlyAuthorityReadsDetails() throws Exception {
        submit(null, body(false, null), jpegPhoto()).andExpect(status().isCreated());
        IllegalReportEntity report = reportRepository.findAll().get(0);
        String photoId = photoRepository.findByReportIdOrderByCreatedAtAsc(report.getId()).get(0).getId();

        mockMvc.perform(get("/api/v1/denuncias").header("Authorization", fisherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/denuncias/" + report.getId()).header("Authorization", fisherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/denuncias/" + report.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/denuncias/" + report.getId() + "/fotos/" + photoId)
                .header("Authorization", fisherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/denuncias").header("Authorization", inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photoCount").value(1));
        mockMvc.perform(get("/api/v1/denuncias/" + report.getId() + "/fotos/" + photoId)
                .header("Authorization", inspectorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-HU10-06 · El reenvío desde la cola offline no duplica la denuncia")
    void offlineResubmissionIsIdempotent() throws Exception {
        String first = extract(submit(null, body(true, "cli-offline-0001")).andReturn().getResponse()
                .getContentAsString(), "trackingCode");
        String second = extract(submit(null, body(true, "cli-offline-0001")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "trackingCode");

        assertThat(second).isEqualTo(first);
        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("La denuncia exige un lugar: coordenadas o una referencia escrita")
    void requiresLocation() throws Exception {
        submit(null, """
                {"reportType":"OTHER","description":"Pesca con dinamita cerca de la costa","occurredOn":"%s",
                 "anonymous":true}""".formatted(AppClock.today()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ------------------------------------------------------------------

    private ResultActions submit(String token, String json, MockMultipartFile... photos) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/denuncias");
        request.file(new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8)));
        for (MockMultipartFile photo : photos) {
            request.file(photo);
        }
        if (token != null) {
            request.header("Authorization", token);
        }
        return mockMvc.perform(request);
    }

    private ResultActions changeStatus(String id, String status, String note) throws Exception {
        return mockMvc.perform(patch("/api/v1/denuncias/" + id + "/estado").header("Authorization", inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s","note":"%s"}""".formatted(status, note)));
    }

    private static String body(boolean anonymous, String clientId) {
        return """
                {%s"reportType":"CLOSURE_FISHING","description":"Embarcación pescando camarón en plena veda",
                 "occurredOn":"%s","latitude":6.21,"longitude":-77.5,"anonymous":%s}"""
                .formatted(clientId == null ? "" : "\"clientId\":\"" + clientId + "\",", AppClock.today(), anonymous);
    }

    private static MockMultipartFile jpegPhoto() throws Exception {
        return new MockMultipartFile("photos", "foto.jpg", "image/jpeg", jpeg());
    }

    private static byte[] jpeg() throws Exception {
        BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        image.setRGB(3, 3, 0x1E6091);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    /** JPEG válido con un segmento APP1/Exif insertado justo después del SOI, como lo deja un teléfono. */
    private static byte[] jpegWithExif() throws Exception {
        byte[] base = jpeg();
        byte[] payload = "Exif\0\0GPS 6.2104N 77.5012W Phone-Model-X".getBytes(StandardCharsets.ISO_8859_1);
        int length = payload.length + 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(base, 0, 2);
        out.write(new byte[] { (byte) 0xFF, (byte) 0xE1, (byte) (length >> 8), (byte) length });
        out.write(payload);
        out.write(base, 2, base.length - 2);
        return out.toByteArray();
    }
}
