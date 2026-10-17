package com.sigepes.backend.documents;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record CreateDocumentRequest(
            @NotBlank(message = "El tipo de documento es obligatorio")
            @Pattern(regexp = "ID_DOCUMENT|VESSEL_CERTIFICATE|PERMIT|RESOLUTION|OTHER", message = "El tipo no es válido")
            String docType,

            @NotBlank(message = "El título es obligatorio")
            @Size(max = 160, message = "El título no puede superar 160 caracteres")
            String title,

            @NotBlank(message = "Indica a qué registro pertenece")
            @Pattern(regexp = "USER|VESSEL|PERMIT|CLOSURE", message = "El tipo de registro no es válido")
            String entityType,

            @NotBlank(message = "Indica a qué registro pertenece")
            String entityId,

            LocalDate expiresOn,

            @Size(max = 300, message = "La nota no puede superar 300 caracteres")
            String note) {
    }

    public record NewVersionRequest(
            LocalDate expiresOn,

            @Size(max = 300, message = "La nota no puede superar 300 caracteres")
            String note) {
    }

    public record VersionResponse(
            int version,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String sha256,
            LocalDate expiresOn,
            String note,
            Instant uploadedAt) {

        static VersionResponse from(DocumentVersionEntity version) {
            return new VersionResponse(version.getVersionNo(), version.getOriginalFilename(), version.getContentType(),
                    version.getSizeBytes(), version.getSha256(), version.getExpiresOn(), version.getNote(),
                    version.getUploadedAt());
        }
    }

    public record DocumentResponse(
            String id,
            String docType,
            String title,
            String entityType,
            String entityId,
            String entityLabel,
            String ownerName,
            int currentVersion,
            LocalDate expiresOn,
            String expiryStatus,
            Instant updatedAt,
            List<VersionResponse> versions) {
    }

    public record DownloadLink(String url, Instant expiresAt) {
    }
}
