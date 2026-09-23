package com.sigepes.backend.permits;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sigepes.backend.permits.PermitDtos.CreatePermitRequest;
import com.sigepes.backend.permits.PermitDtos.EvaluatePermitRequest;
import com.sigepes.backend.permits.PermitDtos.PermitResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-08 · Permisos y licencias. */
@RestController
@RequestMapping("/api/v1/permisos")
@Tag(name = "Permisos")
public class PermitController {

    private final PermitService permitService;

    public PermitController(PermitService permitService) {
        this.permitService = permitService;
    }

    @GetMapping
    @Operation(summary = "Lista los permisos visibles para el usuario autenticado")
    public List<PermitResponse> list() {
        return permitService.findVisible();
    }

    @GetMapping("/validar/{number}")
    @Operation(summary = "Verifica la constancia de un permiso por su número o código de verificación (QR)")
    public PermitResponse validate(@PathVariable String number) {
        return permitService.validateByNumber(number);
    }

    /**
     * AC-08.5: la copia de cédula y el certificado de propiedad de la embarcación son obligatorios.
     * Se reciben como {@code required = false} a propósito: su ausencia se valida a mano en el
     * servicio para responder con el código de error propio de la API, no la excepción cruda que
     * lanzaría Spring por una parte multipart faltante.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita un permiso de pesca con sus adjuntos obligatorios (nace PENDING)")
    public PermitResponse request(
            @RequestPart("request") @Valid CreatePermitRequest request,
            @RequestPart(value = "idDocument", required = false) MultipartFile idDocument,
            @RequestPart(value = "vesselOwnershipDocument", required = false) MultipartFile vesselOwnershipDocument) {
        return permitService.request(request, idDocument, vesselOwnershipDocument);
    }

    @GetMapping("/{id}/adjuntos/{kind}")
    @Operation(summary = "Descarga un adjunto del permiso (solo el titular o la autoridad)")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String id, @PathVariable AttachmentKind kind) {
        PermitAttachmentEntity attachment = permitService.attachment(id, kind);
        Resource resource = permitService.loadAttachment(attachment);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @PatchMapping("/{id}/evaluar")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN')")
    @Operation(summary = "Aprueba, rechaza o suspende un permiso (exige motivo en rechazo y suspensión)")
    public PermitResponse evaluate(@PathVariable String id, @Valid @RequestBody EvaluatePermitRequest request) {
        return permitService.evaluate(id, request);
    }
}
