package com.sigepes.backend.species;

import java.math.BigDecimal;

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
            String status,
            boolean underClosure) {

        public static SpeciesResponse from(SpeciesEntity entity, boolean underClosure) {
            String status = underClosure && entity.getStatus() == SpeciesStatus.ACTIVE
                    ? SpeciesStatus.CLOSED.name()
                    : entity.getStatus().name();
            return new SpeciesResponse(entity.getId(), entity.getCommonName(), entity.getScientificName(),
                    entity.getMinSizeCm(), status, underClosure);
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

            @Pattern(regexp = "ACTIVE|CLOSED|INACTIVE", message = "El estado no es válido")
            String status) {
    }
}
