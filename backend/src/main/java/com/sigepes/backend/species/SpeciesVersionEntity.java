package com.sigepes.backend.species;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * AC-06.4 · Snapshot histórico de una especie en el momento de su alta o de cada edición.
 *
 * <p>Append-only, igual que {@code AuditLogEntity}: nunca se actualiza ni se borra una fila. El
 * actor se guarda como {@code changedBy}/{@code changedByLabel} (id + etiqueta, sin FK dura) por
 * el mismo motivo que {@code audit_logs} lo hace así: la versión debe sobrevivir aunque el usuario
 * que la generó se elimine más adelante.
 */
@Entity
@Table(name = "species_versions")
public class SpeciesVersionEntity {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "species_id", length = 40, nullable = false)
    private String speciesId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "common_name", length = 120, nullable = false)
    private String commonName;

    @Column(name = "scientific_name", length = 160, nullable = false)
    private String scientificName;

    @Column(name = "min_size_cm", precision = 6, scale = 2, nullable = false)
    private BigDecimal minSizeCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_method", length = 20, nullable = false)
    private MeasurementMethod measurementMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 20, nullable = false)
    private SpeciesRegion region;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SpeciesStatus status;

    @Column(name = "changed_by", length = 40)
    private String changedBy;

    @Column(name = "changed_by_label", length = 160, nullable = false)
    private String changedByLabel;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected SpeciesVersionEntity() {
    }

    public SpeciesVersionEntity(String id, String speciesId, int version, String commonName, String scientificName,
            BigDecimal minSizeCm, MeasurementMethod measurementMethod, SpeciesRegion region, String imageUrl,
            SpeciesStatus status, String changedBy, String changedByLabel) {
        this.id = id;
        this.speciesId = speciesId;
        this.version = version;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.minSizeCm = minSizeCm;
        this.measurementMethod = measurementMethod;
        this.region = region;
        this.imageUrl = imageUrl;
        this.status = status;
        this.changedBy = changedBy;
        this.changedByLabel = changedByLabel;
    }

    @PrePersist
    void onCreate() {
        changedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public int getVersion() {
        return version;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public BigDecimal getMinSizeCm() {
        return minSizeCm;
    }

    public MeasurementMethod getMeasurementMethod() {
        return measurementMethod;
    }

    public SpeciesRegion getRegion() {
        return region;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public SpeciesStatus getStatus() {
        return status;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getChangedByLabel() {
        return changedByLabel;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
