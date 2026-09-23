package com.sigepes.backend.species;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class SpeciesDtos {

    private SpeciesDtos() {
    }

    /**
     * Especie tal y como la consume la pantalla "Especies" y el selector de "Nueva faena".
     *
     * <p>{@code status} se calcula: si la especie tiene una veda vigente hoy se informa
     * {@code CLOSED} aunque su estado de catálogo sea {@code ACTIVE}.
     */
    public record SpeciesResponse(
            String id,
            String commonName,
            String scientificName,
            BigDecimal minSizeCm,
            String measurementMethod,
            String region,
            String imageUrl,
            String status,
            boolean underClosure) {

        public static SpeciesResponse from(SpeciesEntity entity, boolean underClosure) {
            String status = underClosure && entity.getStatus() == SpeciesStatus.ACTIVE
                    ? SpeciesStatus.CLOSED.name()
                    : entity.getStatus().name();
            return new SpeciesResponse(entity.getId(), entity.getCommonName(), entity.getScientificName(),
                    entity.getMinSizeCm(), entity.getMeasurementMethod().name(), entity.getRegion().name(),
                    entity.getImageUrl(), status, underClosure);
        }
    }

    /** AC-06.4: una fila del historial de versiones de una especie. */
    public record SpeciesVersionResponse(
            int version,
            String commonName,
            String scientificName,
            BigDecimal minSizeCm,
            String measurementMethod,
            String region,
            String imageUrl,
            String status,
            String changedByLabel,
            Instant changedAt) {

        public static SpeciesVersionResponse from(SpeciesVersionEntity entity) {
            return new SpeciesVersionResponse(entity.getVersion(), entity.getCommonName(),
                    entity.getScientificName(), entity.getMinSizeCm(), entity.getMeasurementMethod().name(),
                    entity.getRegion().name(), entity.getImageUrl(), entity.getStatus().name(),
                    entity.getChangedByLabel(), entity.getChangedAt());
        }
    }

    public record CreateSpeciesRequest(
            @NotBlank(message = "El nombre común es obligatorio")
            @Size(min = 2, max = 120, message = "El nombre común debe tener entre 2 y 120 caracteres")
            String commonName,

            @NotBlank(message = "El nombre científico es obligatorio")
            @Size(min = 2, max = 160, message = "El nombre científico debe tener entre 2 y 160 caracteres")
            String scientificName,

            @NotNull(message = "La talla mínima es obligatoria")
            @DecimalMin(value = "0.1", message = "La talla mínima debe ser mayor que cero")
            BigDecimal minSizeCm,

            @NotBlank(message = "El método de medición es obligatorio")
            @Pattern(regexp = "TOTAL_LENGTH|FORK_LENGTH", message = "El método de medición no es válido")
            String measurementMethod,

            @NotBlank(message = "La región es obligatoria")
            @Pattern(regexp = "PACIFICO|CARIBE|AMBAS", message = "La región no es válida")
            String region,

            @NotBlank(message = "La imagen de referencia es obligatoria")
            @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres")
            String imageUrl,

            @Pattern(regexp = "ACTIVE|CLOSED|INACTIVE", message = "El estado no es válido")
            String status) {
    }

    /** Actualización del catálogo, incluida la talla mínima legal (RF-06). */
    public record UpdateSpeciesRequest(
            @Size(min = 2, max = 120, message = "El nombre común debe tener entre 2 y 120 caracteres")
            String commonName,

            @Size(min = 2, max = 160, message = "El nombre científico debe tener entre 2 y 160 caracteres")
            String scientificName,

            @DecimalMin(value = "0.1", message = "La talla mínima debe ser mayor que cero")
            BigDecimal minSizeCm,

            @Pattern(regexp = "TOTAL_LENGTH|FORK_LENGTH", message = "El método de medición no es válido")
            String measurementMethod,

            @Pattern(regexp = "PACIFICO|CARIBE|AMBAS", message = "La región no es válida")
            String region,

            @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres")
            String imageUrl,

            @Pattern(regexp = "ACTIVE|CLOSED|INACTIVE", message = "El estado no es válido")
            String status) {
    }
}
