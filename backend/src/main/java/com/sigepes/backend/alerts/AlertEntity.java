package com.sigepes.backend.alerts;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** RF-11 · Alerta en el buzón de un usuario. */
@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "recipient_id", length = 40, nullable = false)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 20, nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 10, nullable = false)
    private AlertSeverity severity;

    @Column(name = "title", length = 160, nullable = false)
    private String title;

    @Column(name = "message", length = 600, nullable = false)
    private String message;

    @Column(name = "link", length = 200)
    private String link;

    @Column(name = "source_key", length = 160, nullable = false)
    private String sourceKey;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AlertEntity() {
    }

    public AlertEntity(String id, String recipientId, AlertType alertType, AlertSeverity severity, String title,
            String message, String link, String sourceKey) {
        this.id = id;
        this.recipientId = recipientId;
        this.alertType = alertType;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.link = link;
        this.sourceKey = sourceKey;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void archive() {
        markRead();
        archivedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
