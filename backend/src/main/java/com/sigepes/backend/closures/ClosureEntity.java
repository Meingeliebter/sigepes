package com.sigepes.backend.closures;

import java.time.Instant;
import java.time.LocalDate;

import com.sigepes.backend.species.SpeciesEntity;
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

/** RF-05 · Veda de una especie en una zona y periodo. */
@Entity
@Table(name = "closures")
public class ClosureEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private SpeciesEntity species;

    /** Zona de aplicación. El valor «TODAS» aplica a cualquier zona. */
    @Column(name = "zone", length = 120, nullable = false)
    private String zone;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ClosureStatus status = ClosureStatus.SCHEDULED;

    @Column(name = "reason", length = 400)
    private String reason;

    /** AC-05.4: referencia al documento legal de respaldo (p. ej. número de Resolución AUNAP). */
    @Column(name = "resolution_number", length = 120, nullable = false)
    private String resolutionNumber;

    /** AC-05.5: alcance de la veda (total o excepción de subsistencia). */
    @Enumerated(EnumType.STRING)
    @Column(name = "closure_type", length = 20, nullable = false)
    private ClosureType closureType;

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

    protected ClosureEntity() {
    }

    public ClosureEntity(String id, SpeciesEntity species, String zone, LocalDate startDate, LocalDate endDate,
            ClosureStatus status, String resolutionNumber, ClosureType closureType) {
        this.id = id;
        this.species = species;
        this.zone = zone;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.resolutionNumber = resolutionNumber;
        this.closureType = closureType;
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

    /** ¿Aplica esta zona a la veda? «TODAS» cubre cualquier zona. */
    public boolean coversZone(String requestedZone) {
        return "TODAS".equalsIgnoreCase(zone)
                || (requestedZone != null && zone.equalsIgnoreCase(requestedZone.trim()));
    }

    /** ¿La fecha cae dentro del periodo y la veda no ha sido levantada? */
    public boolean isInForceOn(LocalDate date) {
        return status != ClosureStatus.FINISHED && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** Regla completa: veda vigente para esta especie, fecha y zona (RN-10). */
    public boolean blocks(LocalDate date, String requestedZone) {
        return isInForceOn(date) && coversZone(requestedZone);
    }

    /** Estado presentable: se deriva de las fechas salvo que se haya levantado. */
    public ClosureStatus effectiveStatus(LocalDate today) {
        if (status == ClosureStatus.FINISHED) {
            return ClosureStatus.FINISHED;
        }
        if (today.isBefore(startDate)) {
            return ClosureStatus.SCHEDULED;
        }
        return today.isAfter(endDate) ? ClosureStatus.FINISHED : ClosureStatus.CLOSED;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ClosureStatus getStatus() {
        return status;
    }

    public void setStatus(ClosureStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResolutionNumber() {
        return resolutionNumber;
    }

    public void setResolutionNumber(String resolutionNumber) {
        this.resolutionNumber = resolutionNumber;
    }

    public ClosureType getClosureType() {
        return closureType;
    }

    public void setClosureType(ClosureType closureType) {
        this.closureType = closureType;
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
