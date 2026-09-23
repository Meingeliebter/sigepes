package com.sigepes.backend.closures;

import java.time.LocalDate;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ClosureDtos {

    private ClosureDtos() {
    }

    /**
     * Veda tal y como la consume la pantalla "Vedas".
     *
     * <p>Los nombres {@code from} / {@code to} / {@code species} son los de la interfaz
     * {@code Closure} del cliente; {@code startDate} / {@code endDate} se añaden para consumo
     * automático.
     */
    public record ClosureResponse(
            String id,
            String speciesId,
            String species,
            String zone,
            String from,
            String to,
            String startDate,
            String endDate,
            String status,
            boolean inForce,
            String reason,
            String resolutionNumber,
            String closureType,
            String notes) {

        public static ClosureResponse from(ClosureEntity entity, LocalDate today) {
            String start = entity.getStartDate().toString();
            String end = entity.getEndDate().toString();
            return new ClosureResponse(
                    entity.getId(),
                    entity.getSpecies().getId(),
                    entity.getSpecies().getCommonName(),
                    entity.getZone(),
                    start,
                    end,
                    start,
                    end,
                    entity.effectiveStatus(today).name(),
                    entity.blocks(today, entity.getZone()),
                    entity.getReason(),
                    entity.getResolutionNumber(),
                    entity.getClosureType().name(),
                    entity.getNotes());
        }
    }

    public record CreateClosureRequest(
            @NotBlank(message = "La especie es obligatoria")
            String speciesId,

            @NotBlank(message = "La zona es obligatoria")
            @Size(max = 120, message = "La zona no puede superar 120 caracteres")
            String zone,

            @NotNull(message = "La fecha de inicio es obligatoria")
            LocalDate from,

            @NotNull(message = "La fecha de finalización es obligatoria")
            LocalDate to,

            @Size(max = 400, message = "El motivo no puede superar 400 caracteres")
            String reason,

            @NotBlank(message = "El número de resolución es obligatorio")
            @Size(max = 120, message = "El número de resolución no puede superar 120 caracteres")
            String resolutionNumber,

            @NotBlank(message = "El tipo de veda es obligatorio")
            @Pattern(regexp = "TOTAL|PARTIAL", message = "El tipo de veda no es válido")
            String closureType,

            @Size(max = 400, message = "Las observaciones no pueden superar 400 caracteres")
            String notes) {
    }

    /** Levantamiento o reprogramación de una veda. */
    public record UpdateClosureRequest(
            @Pattern(regexp = "SCHEDULED|CLOSED|FINISHED", message = "El estado no es válido")
            String status,

            LocalDate from,

            LocalDate to,

            @Size(max = 120, message = "El número de resolución no puede superar 120 caracteres")
            String resolutionNumber,

            @Pattern(regexp = "TOTAL|PARTIAL", message = "El tipo de veda no es válido")
            String closureType,

            @Size(max = 400, message = "Las observaciones no pueden superar 400 caracteres")
            String notes) {
    }
}
