package com.sigepes.backend.quotas;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class QuotaDtos {

    private QuotaDtos() {
    }

    /** Cuota con su saldo, tal y como la consumen la pantalla "Cuotas" y la bitácora. */
    public record QuotaResponse(
            String id,
            String speciesId,
            String species,
            String zone,
            String from,
            String to,
            BigDecimal totalKg,
            BigDecimal consumedKg,
            BigDecimal remainingKg,
            BigDecimal consumedPercent,
            String status,
            String vesselId,
            String vessel,
            String resolutionNumber,
            String notes) {

        public static QuotaResponse from(QuotaEntity quota) {
            return new QuotaResponse(
                    quota.getId(),
                    quota.getSpecies().getId(),
                    quota.getSpecies().getCommonName(),
                    quota.getZone(),
                    quota.getPeriodStart().toString(),
                    quota.getPeriodEnd().toString(),
                    quota.getTotalKg(),
                    quota.getConsumedKg(),
                    quota.remainingKg(),
                    quota.consumedPercent(),
                    quota.getStatus().name(),
                    quota.getVessel() == null ? null : quota.getVessel().getId(),
                    quota.getVessel() == null ? null : quota.getVessel().getName(),
                    quota.getResolutionNumber(),
                    quota.getNotes());
        }
    }

    public record CreateQuotaRequest(
            @NotBlank(message = "La especie es obligatoria")
            String speciesId,

            @NotBlank(message = "La zona es obligatoria")
            @Size(max = 120, message = "La zona no puede superar 120 caracteres")
            String zone,

            @NotNull(message = "La fecha de inicio es obligatoria")
            LocalDate from,

            @NotNull(message = "La fecha de finalización es obligatoria")
            LocalDate to,

            @NotNull(message = "El total de la cuota es obligatorio")
            @DecimalMin(value = "0.01", message = "La cuota debe ser mayor que cero")
            BigDecimal totalKg,

            String vesselId,

            @Size(max = 120, message = "El número de resolución no puede superar 120 caracteres")
            String resolutionNumber,

            @Size(max = 400, message = "Las observaciones no pueden superar 400 caracteres")
            String notes) {
    }

    public record UpdateQuotaRequest(
            @DecimalMin(value = "0.01", message = "La cuota debe ser mayor que cero")
            BigDecimal totalKg,

            LocalDate to,

            @Pattern(regexp = "ACTIVE|CLOSED", message = "El estado no es válido")
            String status,

            @Size(max = 400, message = "Las observaciones no pueden superar 400 caracteres")
            String notes) {
    }
}
