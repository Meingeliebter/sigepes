package com.sigepes.backend.reports;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sigepes.backend.reports.ReportDtos.ChangeStatusRequest;
import com.sigepes.backend.reports.ReportDtos.ReportDetail;
import com.sigepes.backend.reports.ReportDtos.ReportReceipt;
import com.sigepes.backend.reports.ReportDtos.ReportSummary;
import com.sigepes.backend.reports.ReportDtos.ReportTrackingResponse;
import com.sigepes.backend.reports.ReportDtos.SubmitReportRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * RF-10 · Denuncias de pesca ilegal.
 *
 * <p>Radicar y consultar por código son públicos (cualquier ciudadano, con o sin cuenta); leer el
 * contenido de una denuncia es exclusivo de INSPECTOR y ADMIN (AC-10.5).
 */
@RestController
@RequestMapping("/api/v1/denuncias")
@Tag(name = "Denuncias")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Radica una denuncia, anónima si se pide, con hasta 3 fotos (se les quita el EXIF)")
    public ReportReceipt submit(
            @RequestPart("request") @Valid SubmitReportRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        return reportService.submit(request, photos);
    }

    @GetMapping("/seguimiento/{code}")
    @Operation(summary = "Consulta pública del estado de una denuncia por su código de seguimiento")
    public ReportTrackingResponse track(@PathVariable String code) {
        return reportService.track(code);
    }

    @GetMapping("/mias")
    @Operation(summary = "Denuncias identificadas del usuario autenticado (las anónimas no tienen vínculo)")
    public List<ReportTrackingResponse> mine() {
        return reportService.mine();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN')")
    @Operation(summary = "Bandeja de denuncias para la autoridad, opcionalmente por estado")
    public List<ReportSummary> queue(@RequestParam(name = "estado", required = false) ReportStatus status) {
        return reportService.queue(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN')")
    @Operation(summary = "Detalle de una denuncia con su historial de gestión")
    public ReportDetail detail(@PathVariable String id) {
        return reportService.detail(id);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN')")
    @Operation(summary = "Mueve la denuncia en el flujo Recibida → En investigación → Cerrada, con nota obligatoria")
    public ReportDetail changeStatus(@PathVariable String id, @Valid @RequestBody ChangeStatusRequest request) {
        return reportService.changeStatus(id, request);
    }

    @GetMapping("/{id}/fotos/{photoId}")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN')")
    @Operation(summary = "Descarga una foto de la denuncia (ya sin metadatos)")
    public ResponseEntity<Resource> photo(@PathVariable String id, @PathVariable String photoId) {
        ReportPhotoEntity photo = reportService.photo(id, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(reportService.loadPhoto(photo));
    }
}
