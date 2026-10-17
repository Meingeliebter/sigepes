package com.sigepes.backend.documents;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sigepes.backend.documents.DocumentDtos.CreateDocumentRequest;
import com.sigepes.backend.documents.DocumentDtos.DocumentResponse;
import com.sigepes.backend.documents.DocumentDtos.DownloadLink;
import com.sigepes.backend.documents.DocumentDtos.NewVersionRequest;
import com.sigepes.backend.documents.DocumentEnums.DocType;
import com.sigepes.backend.documents.DocumentEnums.EntityType;
import com.sigepes.backend.documents.DocumentEnums.ExpiryStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-15 · Gestión documental versionada. */
@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Documentos propios (o todos, para inspectores y administradores), con filtros")
    public List<DocumentResponse> list(
            @RequestParam(name = "tipo", required = false) DocType type,
            @RequestParam(name = "entidad", required = false) EntityType entityType,
            @RequestParam(name = "estado", required = false) ExpiryStatus status) {
        return service.list(type, entityType, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del documento con todas sus versiones")
    public DocumentResponse detail(@PathVariable String id) {
        return service.detail(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Carga un documento (PDF, JPG o PNG hasta 5 MB) vinculado a un registro")
    public DocumentResponse create(@RequestPart("request") @Valid CreateDocumentRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.create(request, file);
    }

    @PostMapping(path = "/{id}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega una versión; las anteriores se conservan (RN-30)")
    public DocumentResponse newVersion(@PathVariable String id,
            @RequestPart(value = "request", required = false) @Valid NewVersionRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.newVersion(id, request == null ? new NewVersionRequest(null, null) : request, file);
    }

    @PostMapping("/{id}/versiones/{version}/enlace")
    @Operation(summary = "Genera un enlace de descarga firmado que caduca en 5 minutos")
    public DownloadLink link(@PathVariable String id, @PathVariable int version) {
        return service.link(id, version);
    }

    @GetMapping("/descarga/{token}")
    @Operation(summary = "Descarga con un enlace firmado vigente (se audita)")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        DocumentService.Download download = service.download(token);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.version().getOriginalFilename(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.version().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(download.resource());
    }
}
