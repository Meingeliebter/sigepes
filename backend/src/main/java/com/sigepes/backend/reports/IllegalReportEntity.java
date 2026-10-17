package com.sigepes.backend.reports;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.sigepes.backend.users.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * RF-10 · Denuncia de pesca ilegal.
 *
 * <p>Si es anónima, {@code reporter} queda en null y la entidad no tiene ningún otro campo que
 * permita reconstruir la identidad del denunciante (RN-20, AC-10.5).
 */
@Entity
@Table(name = "illegal_reports")
public class IllegalReportEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "tracking_code", length = 20, nullable = false, unique = true)
    private String trackingCode;

    @Column(name = "client_id", length = 64, unique = true)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 30, nullable = false)
    private ReportType reportType;

    @Column(name = "description", length = 2000, nullable = false)
    private String description;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "location_text", length = 200)
    private String locationText;

    @Column(name = "anonymous", nullable = false)
    private boolean anonymous;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private UserEntity reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReportStatus status = ReportStatus.RECEIVED;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IllegalReportEntity() {
    }

    /** Con {@code reporter} en null la denuncia nace anónima. */
    public IllegalReportEntity(String id, String trackingCode, ReportType reportType, String description,
            LocalDate occurredOn, UserEntity reporter) {
        this.id = id;
        this.trackingCode = trackingCode;
        this.reportType = reportType;
        this.description = description;
        this.occurredOn = occurredOn;
        this.reporter = reporter;
        this.anonymous = reporter == null;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLocation(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public UserEntity getReporter() {
        return reporter;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
