package com.sigepes.backend.documents;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** RN-30 · Versión de un documento. Inmutable: una corrección es siempre una versión nueva. */
@Entity
@Immutable
@Table(name = "document_versions")
public class DocumentVersionEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "document_id", length = 40, nullable = false)
    private String documentId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "storage_path", length = 300, nullable = false)
    private String storagePath;

    @Column(name = "original_filename", length = 160, nullable = false)
    private String originalFilename;

    @Column(name = "content_type", length = 40, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", length = 64, nullable = false)
    private String sha256;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "note", length = 300)
    private String note;

    @Column(name = "uploaded_by", length = 40, nullable = false)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected DocumentVersionEntity() {
    }

    DocumentVersionEntity(String id, String documentId, int versionNo, String storagePath, String originalFilename,
            String contentType, long sizeBytes, String sha256, LocalDate expiresOn, String note, String uploadedBy) {
        this.id = id;
        this.documentId = documentId;
        this.versionNo = versionNo;
        this.storagePath = storagePath;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.expiresOn = expiresOn;
        this.note = note;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        uploadedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public String getStoragePath() {
        return storagePath;
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

    public String getSha256() {
        return sha256;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public String getNote() {
        return note;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
