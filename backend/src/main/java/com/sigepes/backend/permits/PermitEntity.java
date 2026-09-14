package com.sigepes.backend.permits;

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

/** RF-08 · Permiso o licencia de pesca. */
@Entity
@Table(name = "permits")
public class PermitEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "number", length = 40, nullable = false, unique = true)
    private String number;

    @Column(name = "permit_type", length = 80, nullable = false)
    private String type;

    @Column(name = "holder", length = 160, nullable = false)
    private String holder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PermitStatus status = PermitStatus.PENDING;

    /** Motivo del rechazo o de la suspensión. */
    @Column(name = "reason", length = 400)
    private String reason;

    /** Código de la constancia verificable. */
    @Column(name = "verification_code", length = 40, nullable = false, unique = true)
    private String verificationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PermitEntity() {
    }

    public PermitEntity(String id, String number, String type, String holder, UserEntity owner, LocalDate issuedAt,
            LocalDate expiresAt, PermitStatus status, String verificationCode) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.holder = holder;
        this.owner = owner;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.verificationCode = verificationCode;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** ¿El permiso está vencido en la fecha dada? */
    public boolean isExpiredOn(LocalDate date) {
        return date.isAfter(expiresAt);
    }

    /**
     * RN-16: habilita operaciones solo si está aprobado y la fecha cae dentro de su vigencia.
     */
    public boolean isValidOn(LocalDate date) {
        return status == PermitStatus.APPROVED && !date.isBefore(issuedAt) && !date.isAfter(expiresAt);
    }

    /** Estado presentable: un permiso aprobado pero pasado de fecha se informa como vencido. */
    public PermitStatus effectiveStatus(LocalDate today) {
        if (status == PermitStatus.APPROVED && isExpiredOn(today)) {
            return PermitStatus.EXPIRED;
        }
        return status;
    }

    /** ¿Vence dentro de los próximos {@code days} días? (alerta de RF-08). */
    public boolean isExpiringSoon(LocalDate today, int days) {
        return status == PermitStatus.APPROVED
                && !isExpiredOn(today)
                && !expiresAt.isAfter(today.plusDays(days));
    }

    public String getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDate issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public PermitStatus getStatus() {
        return status;
    }

    public void setStatus(PermitStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public UserEntity getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UserEntity reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
