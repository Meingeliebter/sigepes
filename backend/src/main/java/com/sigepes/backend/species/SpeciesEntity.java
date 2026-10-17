package com.sigepes.backend.species;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** RF-06 · Especie del catálogo con su talla mínima legal. */
@Entity
@Table(name = "species")
public class SpeciesEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "common_name", length = 120, nullable = false, unique = true)
    private String commonName;

    @Column(name = "scientific_name", length = 160, nullable = false)
    private String scientificName;

    /** RN-12: talla mínima legal de captura. */
    @Column(name = "min_size_cm", precision = 6, scale = 2, nullable = false)
    private BigDecimal minSizeCm;

    /** AC-06.2: método con el que se mide la talla (longitud total vs. a la horquilla). */
    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_method", length = 20, nullable = false)
    private MeasurementMethod measurementMethod;

    /** AC-06.5: región donde la especie está presente, para filtrar el catálogo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 20, nullable = false)
    private SpeciesRegion region;

    /** AC-06.2: imagen de referencia para identificar la especie en campo. */
    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SpeciesStatus status = SpeciesStatus.ACTIVE;

    @Column(name = "demo", nullable = false)
    private boolean demo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SpeciesEntity() {
    }

    public SpeciesEntity(String id, String commonName, String scientificName, BigDecimal minSizeCm,
            SpeciesStatus status, MeasurementMethod measurementMethod, SpeciesRegion region, String imageUrl) {
        this.id = id;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.minSizeCm = minSizeCm;
        this.status = status;
        this.measurementMethod = measurementMethod;
        this.region = region;
        this.imageUrl = imageUrl;
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

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public BigDecimal getMinSizeCm() {
        return minSizeCm;
    }

    public void setMinSizeCm(BigDecimal minSizeCm) {
        this.minSizeCm = minSizeCm;
    }

    public MeasurementMethod getMeasurementMethod() {
        return measurementMethod;
    }

    public void setMeasurementMethod(MeasurementMethod measurementMethod) {
        this.measurementMethod = measurementMethod;
    }

    public SpeciesRegion getRegion() {
        return region;
    }

    public void setRegion(SpeciesRegion region) {
        this.region = region;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public SpeciesStatus getStatus() {
        return status;
    }

    public void setStatus(SpeciesStatus status) {
        this.status = status;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
