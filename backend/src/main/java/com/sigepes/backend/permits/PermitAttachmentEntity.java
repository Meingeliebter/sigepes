package com.sigepes.backend.permits;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** AC-08.5 · Documento adjunto a una solicitud de permiso (copia de cédula, propiedad de embarcación). */
@Entity
@Table(name = "permit_attachments")
public class PermitAttachmentEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "permit_id", length = 40, nullable = false)
    private String permitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 20, nullable = false)
    private AttachmentKind kind;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Ruta relativa dentro del directorio de almacenamiento (nunca se expone al cliente). */
    @Column(name = "storage_path", length = 255, nullable = false)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected PermitAttachmentEntity() {
    }

    public PermitAttachmentEntity(String id, String permitId, AttachmentKind kind, String originalFilename,
            String contentType, long sizeBytes, String storagePath) {
        this.id = id;
        this.permitId = permitId;
        this.kind = kind;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
    }

    @PrePersist
    void onCreate() {
        uploadedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getPermitId() {
        return permitId;
    }

    public AttachmentKind getKind() {
        return kind;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
