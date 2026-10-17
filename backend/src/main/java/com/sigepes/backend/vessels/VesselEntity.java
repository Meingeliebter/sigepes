package com.sigepes.backend.vessels;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.users.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** RF-02 · Embarcación. */
@Entity
@Table(name = "vessels")
public class VesselEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "registration", length = 60, nullable = false, unique = true)
    private String registration;

    @Column(name = "port", length = 120, nullable = false)
    private String port;

    @Column(name = "vessel_type", length = 60)
    private String vesselType;

    /** AC-02.2: tipo de casco de la embarcación. */
    @Column(name = "hull_type", length = 60, nullable = false)
    private String hullType;

    /** AC-02.2: potencia del motor, en HP. */
    @Column(name = "engine_power", precision = 10, scale = 2, nullable = false)
    private BigDecimal enginePower;

    @Column(name = "capacity_tons", precision = 10, scale = 2, nullable = false)
    private BigDecimal capacityTons;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private VesselStatus status = VesselStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "vessel_gears",
            joinColumns = @JoinColumn(name = "vessel_id"),
            inverseJoinColumns = @JoinColumn(name = "gear_id"))
    private Set<FishingGearEntity> gears = new LinkedHashSet<>();

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VesselEntity() {
    }

    public VesselEntity(String id, String name, String registration, String port, UserEntity owner) {
        this.id = id;
        this.name = name;
        this.registration = registration;
        this.port = port;
        this.owner = owner;
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistration() {
        return registration;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getVesselType() {
        return vesselType;
    }

    public void setVesselType(String vesselType) {
        this.vesselType = vesselType;
    }

    public String getHullType() {
        return hullType;
    }

    public void setHullType(String hullType) {
        this.hullType = hullType;
    }

    public BigDecimal getEnginePower() {
        return enginePower;
    }

    public void setEnginePower(BigDecimal enginePower) {
        this.enginePower = enginePower;
    }

    public BigDecimal getCapacityTons() {
        return capacityTons;
    }

    public void setCapacityTons(BigDecimal capacityTons) {
        this.capacityTons = capacityTons;
    }

    public VesselStatus getStatus() {
        return status;
    }

    public void setStatus(VesselStatus status) {
        this.status = status;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public Set<FishingGearEntity> getGears() {
        return gears;
    }

    public void setGears(Set<FishingGearEntity> gears) {
        this.gears = gears;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
