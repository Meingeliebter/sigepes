package com.sigepes.backend.reports;

import java.time.Instant;

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
import jakarta.persistence.Table;

/** AC-10.4 · Un paso del flujo de inspección, con su nota obligatoria. */
@Entity
@Table(name = "report_status_changes")
public class ReportStatusChangeEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "report_id", length = 40, nullable = false)
    private String reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, nullable = false)
    private ReportStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, nullable = false)
    private ReportStatus toStatus;

    @Column(name = "note", length = 1000, nullable = false)
    private String note;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private UserEntity changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ReportStatusChangeEntity() {
    }

    public ReportStatusChangeEntity(String id, String reportId, ReportStatus fromStatus, ReportStatus toStatus,
            String note, UserEntity changedBy) {
        this.id = id;
        this.reportId = reportId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.changedBy = changedBy;
    }

    @PrePersist
    void onCreate() {
        changedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public ReportStatus getFromStatus() {
        return fromStatus;
    }

    public ReportStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
