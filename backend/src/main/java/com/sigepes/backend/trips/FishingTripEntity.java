package com.sigepes.backend.trips;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** RF-03 · Faena de la bitácora electrónica. */
@Entity
@Table(name = "fishing_trips")
public class FishingTripEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vessel_id", nullable = false)
    private VesselEntity vessel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gear_id")
    private FishingGearEntity gear;

    /** Permiso que habilitó la faena, resuelto al iniciarla (RN-16). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permit_id")
    private PermitEntity permit;

    @Column(name = "zone", length = 120, nullable = false)
    private String zone;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TripStatus status = TripStatus.IN_PROGRESS;

    /** RN-06 · declaración explícita de captura nula. */
    @Column(name = "null_declaration", nullable = false)
    private boolean nullDeclaration;

    @Column(name = "notes", length = 600)
    private String notes;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<CatchEntity> catches = new ArrayList<>();

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FishingTripEntity() {
    }

    public FishingTripEntity(String id, UserEntity owner, VesselEntity vessel, String zone, LocalDate tripDate) {
        this.id = id;
        this.owner = owner;
        this.vessel = vessel;
        this.zone = zone;
        this.tripDate = tripDate;
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

    public boolean isOpen() {
        return status == TripStatus.DRAFT || status == TripStatus.IN_PROGRESS;
    }

    public void addCatch(CatchEntity entry) {
        catches.add(entry);
        entry.setTrip(this);
    }

    public String getId() {
        return id;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public VesselEntity getVessel() {
        return vessel;
    }

    public FishingGearEntity getGear() {
        return gear;
    }

    public void setGear(FishingGearEntity gear) {
        this.gear = gear;
    }

    public PermitEntity getPermit() {
        return permit;
    }

    public void setPermit(PermitEntity permit) {
        this.permit = permit;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public void setTripDate(LocalDate tripDate) {
        this.tripDate = tripDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public boolean isNullDeclaration() {
        return nullDeclaration;
    }

    public void setNullDeclaration(boolean nullDeclaration) {
        this.nullDeclaration = nullDeclaration;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public List<CatchEntity> getCatches() {
        return catches;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
