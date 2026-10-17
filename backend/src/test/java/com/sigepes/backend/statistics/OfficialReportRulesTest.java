package com.sigepes.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.sigepes.backend.audit.AuditLogRepository;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;

/** HU-18 · RF-18 / RN-36 · Reportes y estadísticas para autoridades. */
class OfficialReportRulesTest extends ApiTestSupport {

    @Autowired
    private OfficialReportService reportService;

    @Autowired
    private AuditLogRepository auditRepository;

    private String fisherToken;
    private String analystToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        UserEntity fisher = givenUser("5151515151", "pescador.rep@sigepes.gov.co", Role.FISHER);
        givenUser("5151515152", "analista.rep@sigepes.gov.co", Role.ANALYST);
        givenUser("5151515153", "inspector.rep@sigepes.gov.co", Role.INSPECTOR);
        VesselEntity vessel = givenVessel(fisher, "DIMAR-REP-001", VesselStatus.ACTIVE, givenGear("Línea rep", false));
        SpeciesEntity snapper = givenSpecies("Pargo reporte", new BigDecimal("35.00"));
        givenPermit(fisher, "PES-REP-1", PermitStatus.APPROVED, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6));
        fisherToken = bearerFor("pescador.rep@sigepes.gov.co");
        analystToken = bearerFor("analista.rep@sigepes.gov.co");
        inspectorToken = bearerFor("inspector.rep@sigepes.gov.co");

        String closed = apiStartedTripId(fisherToken, vessel.getId(), "Caribe");
        apiAddCatch(fisherToken, closed, snapper.getId(), "45", "120").andExpect(status().isOk());
        apiCloseTrip(fisherToken, closed).andExpect(status().isOk());
        apiStartedTripId(fisherToken, vessel.getId(), "Caribe");
    }

    @Test
    @DisplayName("TC-HU18-01 · CSV con encabezado SIGEPES, filtros, usuario y nota de registros excluidos")
    void exportsCsvWithHeaderAndFootnote() throws Exception {
        byte[] body = mockMvc.perform(get("/api/v1/reportes-oficiales/LANDINGS/exportar").param("formato", "CSV")
                        .header("Authorization", analystToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("sigepes_desembarcos_")))
                .andReturn().getResponse().getContentAsByteArray();
        String csv = new String(body, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿SIGEPES");
        assertThat(csv).contains("Desembarcos por especie y zona", "por Usuario ANALYST", "Región: todas",
                "Pargo reporte;Caribe;Caribe;120.00;1", "faenas abiertas o anuladas del periodo): 1.");
        // AC-18.6: ninguna cédula ni correo en el archivo.
        assertThat(csv).doesNotContain("5151515151", "@sigepes.gov.co");
        assertThat(auditRepository.findAll()).anyMatch(entry -> "REPORT_EXPORTED".equals(entry.getAction()));
    }

    @Test
    @DisplayName("TC-HU18-02 · PDF del mismo reporte")
    void exportsPdf() throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/v1/reportes-oficiales/QUOTA_COMPLIANCE/exportar").param("formato", "PDF")
                        .header("Authorization", inspectorToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("TC-HU18-03 · AC-18.6: un pescador no genera reportes oficiales")
    void fishersCannotExport() throws Exception {
        mockMvc.perform(get("/api/v1/reportes-oficiales/LANDINGS/exportar").header("Authorization", fisherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reportes-oficiales").header("Authorization", fisherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-HU18-04 · AC-18.5: más de un año se genera en segundo plano y avisa por el buzón")
    void longExportsRunInBackground() throws Exception {
        String job = mockMvc.perform(get("/api/v1/reportes-oficiales/LANDINGS/exportar").param("formato", "CSV")
                        .param("desde", LocalDate.now().minusYears(2).toString())
                        .param("hasta", LocalDate.now().toString())
                        .header("Authorization", analystToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String jobId = extract(job, "id");

        mockMvc.perform(get("/api/v1/reportes-oficiales/trabajos/" + jobId + "/archivo").header("Authorization", analystToken))
                .andExpect(status().isConflict());

        reportService.runPending();

        mockMvc.perform(get("/api/v1/reportes-oficiales/trabajos").header("Authorization", analystToken))
                .andExpect(jsonPath("$[0].status").value("DONE"))
                .andExpect(jsonPath("$[0].rowCount").value(1));
        mockMvc.perform(get("/api/v1/alertas").header("Authorization", analystToken))
                .andExpect(jsonPath("$.alerts[?(@.type=='EXPORT')].title").value("Tu reporte está listo"));
        mockMvc.perform(get("/api/v1/reportes-oficiales/trabajos/" + jobId + "/archivo").header("Authorization", analystToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
        // El archivo de otro usuario no existe para los demás.
        mockMvc.perform(get("/api/v1/reportes-oficiales/trabajos/" + jobId + "/archivo").header("Authorization", inspectorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-HU18-05 · Denuncias: sin denunciante y con coordenadas redondeadas")
    void illegalReportsHaveNoPersonalData() throws Exception {
        mockMvc.perform(multipart("/api/v1/denuncias").header("Authorization", fisherToken)
                        .file(new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, """
                                {"reportType":"OTHER","description":"Pesca con dinamita frente a la playa","occurredOn":"%s",
                                 "latitude":6.214567,"longitude":-77.501234,"anonymous":false}"""
                                .formatted(LocalDate.now()).getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reportes-oficiales/ILLEGAL_REPORTS/vista").header("Authorization", inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.table.rows[0][5]").value("6.21, -77.50"))
                .andExpect(jsonPath("$.table.rows[0][6]").value("No"))
                .andExpect(jsonPath("$.table.columns", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Denunciante"))));
        String csv = mockMvc.perform(get("/api/v1/reportes-oficiales/ILLEGAL_REPORTS/exportar").header("Authorization", inspectorToken))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).doesNotContain("Usuario FISHER", "5151515151");
    }

    @Test
    @DisplayName("CSV: las celdas que parecen fórmulas se neutralizan; los números negativos no")
    void csvInjectionIsNeutralized() {
        assertThat(ReportRenderer.cell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(ReportRenderer.cell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(ReportRenderer.cell("-77.52")).isEqualTo("-77.52");
        assertThat(ReportRenderer.cell("6.21, -77.50")).isEqualTo("6.21, -77.50");
    }
}
