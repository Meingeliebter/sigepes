package com.sigepes.backend.gears;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** RF-02 · Arte de pesca. */
@Entity
@Table(name = "fishing_gears")
public class FishingGearEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "name", length = 120, nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 400)
    private String description;

    /** RN-04: un arte destructivo o prohibido no puede registrarse en una embarcación. */
    @Column(name = "destructive", nullable = false)
    private boolean destructive;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GearStatus status = GearStatus.ACTIVE;

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FishingGearEntity() {
    }

    public FishingGearEntity(String id, String name, String description, boolean destructive, GearStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.destructive = destructive;
        this.status = status;
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

    /** Un arte solo puede usarse si no es destructivo y está activo. */
    public boolean isUsable() {
        return !destructive && status == GearStatus.ACTIVE;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDestructive() {
        return destructive;
    }

    public void setDestructive(boolean destructive) {
        this.destructive = destructive;
    }

    public GearStatus getStatus() {
        return status;
    }

    public void setStatus(GearStatus status) {
        this.status = status;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
