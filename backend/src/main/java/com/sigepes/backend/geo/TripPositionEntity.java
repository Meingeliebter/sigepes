package com.sigepes.backend.geo;

import java.math.BigDecimal;
import java.time.Instant;

import com.sigepes.backend.trips.FishingTripEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** RF-04 · Posición GPS registrada durante una faena (AC-04.1). */
@Entity
@Table(name = "trip_positions")
public class TripPositionEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private FishingTripEntity trip;

    @Column(name = "latitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "accuracy_m", precision = 8, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inside_zone_id")
    private RestrictedZoneEntity insideZone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TripPositionEntity() {
    }

    public TripPositionEntity(String id, FishingTripEntity trip, BigDecimal latitude, BigDecimal longitude,
            BigDecimal accuracyMeters, Instant recordedAt) {
        this.id = id;
        this.trip = trip;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.recordedAt = recordedAt;
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

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getAccuracyMeters() {
        return accuracyMeters;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public RestrictedZoneEntity getInsideZone() {
        return insideZone;
    }

    public void setInsideZone(RestrictedZoneEntity insideZone) {
        this.insideZone = insideZone;
    }
}
