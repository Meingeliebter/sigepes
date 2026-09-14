package com.sigepes.backend.permits;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PermitDtos {

    private PermitDtos() {
    }

    /**
     * Permiso tal y como lo consume la tabla de la pantalla "Permisos".
     *
     * <p>{@code number} / {@code type} / {@code holder} / {@code issued} / {@code expires} son los
     * nombres de la interfaz {@code Permit} del cliente.
     */
    public record PermitResponse(
            String id,
            String number,
            String type,
            String holder,
            String issued,
            String expires,
            String status,
            String ownerId,
            String reason,
            String verificationCode,
            boolean expiringSoon,
            Integer daysToExpire) {

        public static PermitResponse from(PermitEntity entity, LocalDate today, int warningDays) {
            PermitStatus effective = entity.effectiveStatus(today);
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, entity.getExpiresAt());
            return new PermitResponse(
                    entity.getId(),
                    entity.getNumber(),
                    entity.getType(),
                    entity.getHolder(),
                    entity.getIssuedAt().toString(),
                    entity.getExpiresAt().toString(),
                    effective.name(),
                    entity.getOwner().getId(),
                    entity.getReason(),
                    entity.getVerificationCode(),
                    entity.isExpiringSoon(today, warningDays),
                    (int) days);
        }
    }

    /**
     * Solicitud de permiso.
     *
     * <p>Las fechas y el estado son opcionales: una solicitud nace {@code PENDING} y, si no se
     * indica vigencia, se propone un año desde hoy. Solo la autoridad puede fijar el estado.
     */
    public record CreatePermitRequest(
            @Size(max = 40, message = "El número no puede superar 40 caracteres")
            String number,

            @NotBlank(message = "El tipo de permiso es obligatorio")
            @Size(max = 80, message = "El tipo no puede superar 80 caracteres")
            String type,

            @Size(max = 160, message = "El titular no puede superar 160 caracteres")
            String holder,

            LocalDate issued,

            LocalDate expires,

            @Pattern(regexp = "PENDING|APPROVED|REJECTED|SUSPENDED", message = "El estado no es válido")
            String status,

            /** Solo un administrador puede solicitar a nombre de otro usuario. */
            String ownerId) {
    }

    /** Resolución de la autoridad: aprobar, rechazar o suspender. */
    public record EvaluatePermitRequest(
            @NotBlank(message = "El estado es obligatorio")
            @Pattern(regexp = "PENDING|APPROVED|REJECTED|SUSPENDED", message = "El estado no es válido")
            String status,

            @Size(max = 400, message = "El motivo no puede superar 400 caracteres")
            String reason,

            /** Permite ajustar la vigencia al aprobar. */
            LocalDate issued,

            LocalDate expires) {
    }
}
