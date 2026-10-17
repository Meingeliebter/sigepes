package com.sigepes.backend.reports;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record SubmitReportRequest(
            @Pattern(regexp = "[A-Za-z0-9_-]{8,64}", message = "El identificador del dispositivo no es válido")
            String clientId,

            @NotBlank(message = "El tipo de denuncia es obligatorio")
            @Pattern(regexp = "CLOSURE_FISHING|UNDERSIZED_CATCH|PROHIBITED_GEAR|MPA_INTRUSION|OTHER",
                    message = "El tipo de denuncia no es válido")
            String reportType,

            @NotBlank(message = "Describe lo que observaste")
            @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
            String description,

            @NotNull(message = "La fecha del hecho es obligatoria")
            @PastOrPresent(message = "La fecha del hecho no puede estar en el futuro")
            LocalDate occurredOn,

            @DecimalMin(value = "-90", message = "Latitud fuera de rango")
            @DecimalMax(value = "90", message = "Latitud fuera de rango")
            BigDecimal latitude,

            @DecimalMin(value = "-180", message = "Longitud fuera de rango")
            @DecimalMax(value = "180", message = "Longitud fuera de rango")
            BigDecimal longitude,

            @Size(max = 200, message = "La referencia del lugar no puede superar 200 caracteres")
            String locationText,

            boolean anonymous) {
    }

    /** Lo único que recibe quien denuncia: con el código puede consultar el estado (AC-10.3). */
    public record ReportReceipt(String trackingCode, String status, boolean anonymous, Instant createdAt) {

        static ReportReceipt from(IllegalReportEntity report) {
            return new ReportReceipt(report.getTrackingCode(), report.getStatus().name(), report.isAnonymous(),
                    report.getCreatedAt());
        }
    }

    /**
     * Consulta pública por código. Solo estado y fechas: la descripción, la ubicación, las fotos y
     * las notas de la investigación quedan reservadas a INSPECTOR y ADMIN (AC-10.5).
     */
    public record ReportTrackingResponse(
            String trackingCode,
            String reportType,
            String status,
            LocalDate occurredOn,
            Instant createdAt,
            Instant updatedAt) {

        static ReportTrackingResponse from(IllegalReportEntity report) {
            return new ReportTrackingResponse(report.getTrackingCode(), report.getReportType().name(),
                    report.getStatus().name(), report.getOccurredOn(), report.getCreatedAt(), report.getUpdatedAt());
        }
    }

    /** Fila de la bandeja de los inspectores. */
    public record ReportSummary(
            String id,
            String trackingCode,
            String reportType,
            String status,
            String description,
            LocalDate occurredOn,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationText,
            boolean anonymous,
            int photoCount,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record StatusChangeResponse(String fromStatus, String toStatus, String note, String changedBy,
            Instant changedAt) {

        static StatusChangeResponse from(ReportStatusChangeEntity change) {
            return new StatusChangeResponse(change.getFromStatus().name(), change.getToStatus().name(),
                    change.getNote(), change.getChangedBy().getFullName(), change.getChangedAt());
        }
    }

    /** Detalle completo, solo para INSPECTOR y ADMIN. */
    public record ReportDetail(
            String id,
            String trackingCode,
            String reportType,
            String status,
            String description,
            LocalDate occurredOn,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationText,
            boolean anonymous,
            /** Nombre del denunciante identificado; null si la denuncia es anónima. */
            String reporterName,
            String resolutionNote,
            List<String> photoIds,
            List<StatusChangeResponse> history,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ChangeStatusRequest(
            @NotBlank(message = "El nuevo estado es obligatorio")
            @Pattern(regexp = "INVESTIGATING|CLOSED_FOUNDED|CLOSED_UNFOUNDED", message = "El estado no es válido")
            String status,

            @NotBlank(message = "La nota de gestión es obligatoria")
            @Size(min = 5, max = 1000, message = "La nota debe tener entre 5 y 1000 caracteres")
            String note) {
    }
}
