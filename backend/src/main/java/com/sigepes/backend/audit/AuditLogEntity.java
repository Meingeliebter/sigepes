package com.sigepes.backend.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entrada de la bitácora de auditoría (RN-40).
 *
 * <p>Es append-only: no se expone ninguna operación de actualización ni de borrado, ni por API ni
 * por servicio.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", length = 40)
    private String actorId;

    @Column(name = "actor_label", length = 160, nullable = false)
    private String actorLabel;

    @Column(name = "action", length = 60, nullable = false)
    private String action;

    /** AC-20.2: tipo de acción normalizado (CREATE/UPDATE/DELETE/SECURITY). */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 20, nullable = false)
    private AuditActionType actionType;

    @Column(name = "module", length = 40, nullable = false)
    private String module;

    @Column(name = "entity", length = 80, nullable = false)
    private String entity;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    @Column(name = "detail", length = 600)
    private String detail;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "before_state", length = 2000)
    private String beforeState;

    @Column(name = "after_state", length = 2000)
    private String afterState;

    protected AuditLogEntity() {
    }

    AuditLogEntity(String id, Instant occurredAt, String actorId, String actorLabel, AuditActionType actionType,
            String action, String module, String entity, String entityId, String detail, String ipAddress,
            String beforeState, String afterState) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.actorLabel = actorLabel;
        this.actionType = actionType;
        this.action = action;
        this.module = module;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.beforeState = beforeState;
        this.afterState = afterState;
    }

    public String getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorLabel() {
        return actorLabel;
    }

    public String getAction() {
        return action;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public String getModule() {
        return module;
    }

    public String getEntity() {
        return entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public String getAfterState() {
        return afterState;
    }
}
