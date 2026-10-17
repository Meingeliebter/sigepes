package com.sigepes.backend.trips;

import java.math.BigDecimal;
import java.time.Instant;

import com.sigepes.backend.species.SpeciesEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** RF-03 · Captura registrada dentro de una faena. */
@Entity
@Table(name = "catches")
public class CatchEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private FishingTripEntity trip;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private SpeciesEntity species;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "weight_kg", precision = 10, scale = 2, nullable = false)
    private BigDecimal weightKg;

    @Column(name = "average_size_cm", precision = 6, scale = 2, nullable = false)
    private BigDecimal averageSizeCm;

    /** AC-06.4: talla mínima vigente al momento de la captura, para no alterar el historial. */
    @Column(name = "min_size_cm_at_capture", precision = 6, scale = 2, nullable = false)
    private BigDecimal minSizeCmAtCapture;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CatchEntity() {
    }

    public CatchEntity(String id, SpeciesEntity species, Integer quantity, BigDecimal weightKg,
            BigDecimal averageSizeCm, BigDecimal minSizeCmAtCapture) {
        this.id = id;
        this.species = species;
        this.quantity = quantity;
        this.weightKg = weightKg;
        this.averageSizeCm = averageSizeCm;
        this.minSizeCmAtCapture = minSizeCmAtCapture;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public FishingTripEntity getTrip() {
        return trip;
    }

    void setTrip(FishingTripEntity trip) {
        this.trip = trip;
    }

    public SpeciesEntity getSpecies() {
        return species;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public BigDecimal getAverageSizeCm() {
        return averageSizeCm;
    }

    public BigDecimal getMinSizeCmAtCapture() {
        return minSizeCmAtCapture;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
