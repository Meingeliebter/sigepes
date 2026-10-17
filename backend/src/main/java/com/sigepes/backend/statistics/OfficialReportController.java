package com.sigepes.backend.statistics;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.indicators.Region;
import com.sigepes.backend.statistics.OfficialReportService.CatalogEntry;
import com.sigepes.backend.statistics.OfficialReportService.ExportResult;
import com.sigepes.backend.statistics.OfficialReportService.JobFile;
import com.sigepes.backend.statistics.OfficialReportService.JobResponse;
import com.sigepes.backend.statistics.OfficialReportService.Preview;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** RF-18 · Reportes oficiales; solo INSPECTOR, ANALYST y ADMIN (AC-18.6). */
@RestController
@RequestMapping("/api/v1/reportes-oficiales")
@PreAuthorize("hasAnyRole('INSPECTOR','ANALYST','ADMIN')")
@Tag(name = "Reportes oficiales")
public class OfficialReportController {

    private final OfficialReportService service;

    public OfficialReportController(OfficialReportService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Catálogo de reportes y filtros que admite cada uno")
    public List<CatalogEntry> catalog() {
        return service.catalog();
    }

    @GetMapping("/{kind}/vista")
    @Operation(summary = "Vista previa del reporte (primeras 200 filas)")
    public Preview preview(@PathVariable OfficialReportKind kind,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "region", required = false) Region region,
            @RequestParam(name = "especieId", required = false) String speciesId) {
        return service.preview(kind, ReportFilters.of(from, to, region, speciesId));
    }

    @GetMapping("/{kind}/exportar")
    @Operation(summary = "Exporta a CSV o PDF; si el periodo supera un año responde 202 y se genera en segundo plano")
    public ResponseEntity<?> export(@PathVariable OfficialReportKind kind,
            @RequestParam(name = "formato", defaultValue = "CSV") ExportJobEntity.Format format,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "region", required = false) Region region,
            @RequestParam(name = "especieId", required = false) String speciesId) {
        ExportResult result = service.export(kind, format, ReportFilters.of(from, to, region, speciesId));
        if (result.job() != null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.job());
        }
        return file(result.content(), result.filename(), result.contentType());
    }

    @GetMapping("/trabajos")
    @Operation(summary = "Exportaciones en segundo plano del usuario")
    public List<JobResponse> jobs() {
        return service.jobs();
    }

    @GetMapping("/trabajos/{id}/archivo")
    @Operation(summary = "Descarga el archivo de una exportación terminada")
    public ResponseEntity<?> jobFile(@PathVariable String id) {
        JobFile file = service.jobFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition(file.filename()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(file.resource());
    }

    private static ResponseEntity<byte[]> file(byte[] content, String filename, String contentType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType + ("text/csv".equals(contentType) ? ";charset=UTF-8" : "")))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition(filename))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(content);
    }

    private static String disposition(String filename) {
        return ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString();
    }
}
