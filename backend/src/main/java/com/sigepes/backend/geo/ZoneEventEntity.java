package com.sigepes.backend.geo;

import java.math.BigDecimal;
import java.time.Instant;

import com.sigepes.backend.trips.FishingTripEntity;
import com.sigepes.backend.users.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** RN-08 · Evento de control: una faena entró a una zona restringida. */
@Entity
@Table(name = "zone_events")
public class ZoneEventEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private FishingTripEntity trip;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private RestrictedZoneEntity zone;

    @Column(name = "position_id", length = 40, nullable = false)
    private String positionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "latitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ZoneEventEntity() {
    }

    public ZoneEventEntity(String id, FishingTripEntity trip, RestrictedZoneEntity zone, TripPositionEntity position) {
        this.id = id;
        this.trip = trip;
        this.zone = zone;
        this.positionId = position.getId();
        this.owner = trip.getOwner();
        this.latitude = position.getLatitude();
        this.longitude = position.getLongitude();
        this.occurredAt = position.getRecordedAt();
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

    public RestrictedZoneEntity getZone() {
        return zone;
    }

    public String getPositionId() {
        return positionId;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
