package com.sigepes.backend.reports;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Foto de una denuncia, guardada ya sin metadatos EXIF ni nombre original (AC-10.5). */
@Entity
@Table(name = "report_photos")
public class ReportPhotoEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "report_id", length = 40, nullable = false)
    private String reportId;

    @Column(name = "storage_path", length = 300, nullable = false)
    private String storagePath;

    @Column(name = "content_type", length = 40, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReportPhotoEntity() {
    }

    public ReportPhotoEntity(String id, String reportId, String storagePath, String contentType, long sizeBytes) {
        this.id = id;
        this.reportId = reportId;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getReportId() {
        return reportId;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
