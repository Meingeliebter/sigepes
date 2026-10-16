package com.sigepes.backend.quotas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;

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
 * RF-07 · Cuota de captura de una especie en una zona y periodo.
 *
 * <p>El saldo se lleva acumulado en {@code consumedKg} (táctica de rendimiento AC-07.5): consultar
 * el saldo no recorre las capturas, y el descuento se hace bajo bloqueo pesimista de la fila para
 * que dos cierres concurrentes no pierdan ni dupliquen kilos.
 */
@Entity
@Table(name = "quotas")
public class QuotaEntity {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private SpeciesEntity species;

    /** Zona de aplicación. «TODAS» aplica a cualquier zona. */
    @Column(name = "zone", length = 120, nullable = false)
    private String zone;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_kg", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalKg;

    @Column(name = "consumed_kg", precision = 12, scale = 2, nullable = false)
    private BigDecimal consumedKg = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vessel_id")
    private VesselEntity vessel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QuotaStatus status = QuotaStatus.ACTIVE;

    @Column(name = "last_threshold", nullable = false)
    private int lastThreshold;

    @Column(name = "resolution_number", length = 120)
    private String resolutionNumber;

    @Column(name = "notes", length = 400)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QuotaEntity() {
    }

    public QuotaEntity(String id, SpeciesEntity species, String zone, LocalDate periodStart, LocalDate periodEnd,
            BigDecimal totalKg) {
        this.id = id;
        this.species = species;
        this.zone = zone;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalKg = totalKg;
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

    /** ¿Aplica a esta captura? Misma especie, zona cubierta, fecha en periodo y cuota activa. */
    public boolean appliesTo(String speciesId, String requestedZone, LocalDate date, String vesselId) {
        return status == QuotaStatus.ACTIVE
                && species.getId().equals(speciesId)
                && coversZone(requestedZone)
                && !date.isBefore(periodStart) && !date.isAfter(periodEnd)
                && (vessel == null || vessel.getId().equals(vesselId));
    }

    public boolean coversZone(String requestedZone) {
        return "TODAS".equalsIgnoreCase(zone)
                || (requestedZone != null && zone.equalsIgnoreCase(requestedZone.trim()));
    }

    public BigDecimal remainingKg() {
        BigDecimal remaining = totalKg.subtract(consumedKg);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    /** Porcentaje consumido, redondeado a una cifra decimal. */
    public BigDecimal consumedPercent() {
        return consumedKg.multiply(HUNDRED).divide(totalKg, 1, RoundingMode.HALF_UP);
    }

    public boolean canAbsorb(BigDecimal weightKg) {
        return consumedKg.add(weightKg).compareTo(totalKg) <= 0;
    }

    /** Descuenta un desembarque. Quien llama ya validó {@link #canAbsorb}. */
    public void consume(BigDecimal weightKg) {
        consumedKg = consumedKg.add(weightKg);
    }

    /** Más específica primero: una cuota asignada a la embarcación gana a la general. */
    public int specificity() {
        return (vessel != null ? 2 : 0) + ("TODAS".equalsIgnoreCase(zone) ? 0 : 1);
    }

    public String getId() {
        return id;
    }

    public SpeciesEntity getSpecies() {
        return species;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTotalKg() {
        return totalKg;
    }

    public void setTotalKg(BigDecimal totalKg) {
        this.totalKg = totalKg;
    }

    public BigDecimal getConsumedKg() {
        return consumedKg;
    }

    public VesselEntity getVessel() {
        return vessel;
    }

    public void setVessel(VesselEntity vessel) {
        this.vessel = vessel;
    }

    public QuotaStatus getStatus() {
        return status;
    }

    public void setStatus(QuotaStatus status) {
        this.status = status;
    }

    public int getLastThreshold() {
        return lastThreshold;
    }

    public void setLastThreshold(int lastThreshold) {
        this.lastThreshold = lastThreshold;
    }

    public String getResolutionNumber() {
        return resolutionNumber;
    }

    public void setResolutionNumber(String resolutionNumber) {
        this.resolutionNumber = resolutionNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserEntity createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
