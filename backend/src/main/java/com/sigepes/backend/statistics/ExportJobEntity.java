package com.sigepes.backend.statistics;

import java.time.Instant;
import java.time.LocalDate;

import com.sigepes.backend.indicators.Region;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** AC-18.5 · Exportación en segundo plano y su resultado. */
@Entity
@Table(name = "export_jobs")
public class ExportJobEntity {

    public enum Status { PENDING, RUNNING, DONE, FAILED }

    public enum Format { CSV, PDF }

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "requested_by", length = 40, nullable = false)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_kind", length = 30, nullable = false)
    private OfficialReportKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 5, nullable = false)
    private Format format;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 20)
    private Region region;

    @Column(name = "species_id", length = 40)
    private String speciesId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "storage_path", length = 300)
    private String storagePath;

    @Column(name = "filename", length = 160)
    private String filename;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "error", length = 300)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected ExportJobEntity() {
    }

    ExportJobEntity(String id, String requestedBy, OfficialReportKind kind, Format format, ReportFilters filters) {
        this.id = id;
        this.requestedBy = requestedBy;
        this.kind = kind;
        this.format = format;
        this.periodFrom = filters.from();
        this.periodTo = filters.to();
        this.region = filters.region();
        this.speciesId = filters.speciesId();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    ReportFilters filters() {
        return new ReportFilters(periodFrom, periodTo, region, speciesId);
    }

    void start() {
        status = Status.RUNNING;
    }

    void finish(String storagePath, String filename, int rowCount) {
        this.status = Status.DONE;
        this.storagePath = storagePath;
        this.filename = filename;
        this.rowCount = rowCount;
        this.finishedAt = Instant.now();
    }

    void fail(String error) {
        this.status = Status.FAILED;
        this.error = error == null ? "Error desconocido" : error.substring(0, Math.min(error.length(), 300));
        this.finishedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public OfficialReportKind getKind() {
        return kind;
    }

    public Format getFormat() {
        return format;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public Status getStatus() {
        return status;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getFilename() {
        return filename;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
