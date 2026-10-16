package com.sigepes.backend.geo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import jakarta.persistence.Transient;

/** RF-04 · Zona restringida (AMP o zona en veda) delimitada por un polígono. */
@Entity
@Table(name = "restricted_zones")
public class RestrictedZoneEntity {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "name", length = 160, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", length = 20, nullable = false)
    private ZoneType zoneType;

    @Column(name = "polygon", length = 12000, nullable = false)
    private String polygonJson;

    @Column(name = "min_lat", precision = 9, scale = 6, nullable = false)
    private BigDecimal minLat;

    @Column(name = "max_lat", precision = 9, scale = 6, nullable = false)
    private BigDecimal maxLat;

    @Column(name = "min_lon", precision = 9, scale = 6, nullable = false)
    private BigDecimal minLon;

    @Column(name = "max_lon", precision = 9, scale = 6, nullable = false)
    private BigDecimal maxLon;

    @Column(name = "resolution_number", length = 120, nullable = false)
    private String resolutionNumber;

    @Column(name = "description", length = 400)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private Polygon polygon;

    protected RestrictedZoneEntity() {
    }

    public RestrictedZoneEntity(String id, String name, ZoneType zoneType, Polygon polygon, String resolutionNumber) {
        this.id = id;
        this.name = name;
        this.zoneType = zoneType;
        this.resolutionNumber = resolutionNumber;
        setPolygon(polygon);
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

    public Polygon polygon() {
        if (polygon == null) {
            try {
                polygon = new Polygon(JSON.readValue(polygonJson, new TypeReference<List<double[]>>() {
                }));
            } catch (Exception e) {
                throw new IllegalStateException("Polígono corrupto en la zona " + id, e);
            }
        }
        return polygon;
    }

    public final void setPolygon(Polygon polygon) {
        this.polygon = polygon;
        try {
            this.polygonJson = JSON.writeValueAsString(polygon.vertices());
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible serializar el polígono", e);
        }
        this.minLat = scale(polygon.minLat());
        this.maxLat = scale(polygon.maxLat());
        this.minLon = scale(polygon.minLon());
        this.maxLon = scale(polygon.maxLon());
    }

    /** AC-04.5: el prefiltro de caja envolvente ya lo hizo la consulta; aquí va la prueba exacta. */
    public boolean contains(double lat, double lon) {
        return active && polygon().contains(lat, lon);
    }

    private static BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
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

    public ZoneType getZoneType() {
        return zoneType;
    }

    public void setZoneType(ZoneType zoneType) {
        this.zoneType = zoneType;
    }

    public String getResolutionNumber() {
        return resolutionNumber;
    }

    public void setResolutionNumber(String resolutionNumber) {
        this.resolutionNumber = resolutionNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
