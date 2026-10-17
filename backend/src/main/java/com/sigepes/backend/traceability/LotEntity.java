package com.sigepes.backend.traceability;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.trips.CatchEntity;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** RF-09 · Lote de producto con origen legal verificado (RN-18). */
@Entity
@Table(name = "lots")
public class LotEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "code", length = 20, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catch_id", nullable = false)
    private CatchEntity catchEntry;

    @Column(name = "trip_id", length = 40, nullable = false)
    private String tripId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private SpeciesEntity species;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vessel_id", nullable = false)
    private VesselEntity vessel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "weight_kg", precision = 12, scale = 2, nullable = false)
    private BigDecimal weightKg;

    @Column(name = "landing_date", nullable = false)
    private LocalDate landingDate;

    @Column(name = "zone", length = 80, nullable = false)
    private String zone;

    @Column(name = "legal_checks", length = 2000, nullable = false)
    private String legalChecks;

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LotEntity() {
    }

    public LotEntity(String id, String code, CatchEntity catchEntry, BigDecimal weightKg, String legalChecks) {
        this.id = id;
        this.code = code;
        this.catchEntry = catchEntry;
        this.tripId = catchEntry.getTrip().getId();
        this.species = catchEntry.getSpecies();
        this.vessel = catchEntry.getTrip().getVessel();
        this.owner = catchEntry.getTrip().getOwner();
        this.weightKg = weightKg;
        this.landingDate = catchEntry.getTrip().getTripDate();
        this.zone = catchEntry.getTrip().getZone();
        this.legalChecks = legalChecks;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public CatchEntity getCatchEntry() {
        return catchEntry;
    }

    public String getTripId() {
        return tripId;
    }

    public SpeciesEntity getSpecies() {
        return species;
    }

    public VesselEntity getVessel() {
        return vessel;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public LocalDate getLandingDate() {
        return landingDate;
    }

    public String getZone() {
        return zone;
    }

    public String getLegalChecks() {
        return legalChecks;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
