package com.sigepes.backend.documents;

import java.time.Instant;
import java.time.LocalDate;

import com.sigepes.backend.documents.DocumentEnums.DocType;
import com.sigepes.backend.documents.DocumentEnums.EntityType;
import com.sigepes.backend.documents.DocumentEnums.ExpiryStatus;
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

/** RF-15 · Documento con historial de versiones (RN-30). */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    /** AC-15.3: días antes del vencimiento en que el documento pasa a "por vencer". */
    public static final int EXPIRING_DAYS = 30;

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", length = 30, nullable = false)
    private DocType docType;

    @Column(name = "title", length = 160, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 20, nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", length = 40, nullable = false)
    private String entityId;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentEntity() {
    }

    public DocumentEntity(String id, UserEntity owner, DocType docType, String title, EntityType entityType,
            String entityId) {
        this.id = id;
        this.owner = owner;
        this.docType = docType;
        this.title = title;
        this.entityType = entityType;
        this.entityId = entityId;
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

    public ExpiryStatus expiryStatus(LocalDate today) {
        if (expiresOn == null) {
            return ExpiryStatus.NO_EXPIRY;
        }
        if (expiresOn.isBefore(today)) {
            return ExpiryStatus.EXPIRED;
        }
        return expiresOn.isAfter(today.plusDays(EXPIRING_DAYS)) ? ExpiryStatus.VALID : ExpiryStatus.EXPIRING;
    }

    /** Registra una nueva versión como vigente; las anteriores se conservan intactas. */
    void advanceTo(int version, LocalDate versionExpiresOn) {
        this.currentVersion = version;
        this.expiresOn = versionExpiresOn;
    }

    public String getId() {
        return id;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public DocType getDocType() {
        return docType;
    }

    public String getTitle() {
        return title;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
