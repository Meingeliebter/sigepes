package com.sigepes.backend.geo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class GeoDtos {

    private GeoDtos() {
    }

    public record ZoneResponse(
            String id,
            String name,
            String zoneType,
            List<double[]> polygon,
            String resolutionNumber,
            String description,
            boolean active) {

        public static ZoneResponse from(RestrictedZoneEntity zone) {
            return new ZoneResponse(zone.getId(), zone.getName(), zone.getZoneType().name(),
                    zone.polygon().vertices(), zone.getResolutionNumber(), zone.getDescription(), zone.isActive());
        }
    }

    public record CreateZoneRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String name,

            @NotBlank(message = "El tipo de zona es obligatorio")
            @Pattern(regexp = "CLOSURE_ZONE|MPA|OTHER", message = "El tipo de zona no es válido")
            String zoneType,

            @NotNull(message = "El polígono es obligatorio")
            @Size(min = 3, max = 300, message = "El polígono debe tener entre 3 y 300 vértices")
            List<double[]> polygon,

            @NotBlank(message = "La referencia legal es obligatoria")
            @Size(max = 120, message = "La referencia legal no puede superar 120 caracteres")
            String resolutionNumber,

            @Size(max = 400, message = "La descripción no puede superar 400 caracteres")
            String description) {
    }

    public record UpdateZoneRequest(
            Boolean active,

            @Size(max = 400, message = "La descripción no puede superar 400 caracteres")
            String description) {
    }

    /** AC-04.1 · Una posición; el id lo genera el cliente para que reintentar no duplique (AC-04.6). */
    public record PositionRequest(
            @Size(max = 40, message = "El identificador no puede superar 40 caracteres")
            String id,

            @NotNull(message = "La latitud es obligatoria")
            @DecimalMin(value = "-90", message = "Latitud inválida")
            @DecimalMax(value = "90", message = "Latitud inválida")
            BigDecimal latitude,

            @NotNull(message = "La longitud es obligatoria")
            @DecimalMin(value = "-180", message = "Longitud inválida")
            @DecimalMax(value = "180", message = "Longitud inválida")
            BigDecimal longitude,

            BigDecimal accuracyMeters,

            Instant recordedAt) {
    }

    public record PositionBatchRequest(
            @NotEmpty(message = "Envía al menos una posición")
            @Size(max = 500, message = "Máximo 500 posiciones por envío")
            List<@Valid PositionRequest> positions) {
    }

    public record PositionResponse(
            String id,
            double latitude,
            double longitude,
            Double accuracyMeters,
            String recordedAt,
            String insideZoneId,
            String insideZone) {

        public static PositionResponse from(TripPositionEntity position) {
            RestrictedZoneEntity zone = position.getInsideZone();
            return new PositionResponse(position.getId(), position.getLatitude().doubleValue(),
                    position.getLongitude().doubleValue(),
                    position.getAccuracyMeters() == null ? null : position.getAccuracyMeters().doubleValue(),
                    position.getRecordedAt().toString(), zone == null ? null : zone.getId(),
                    zone == null ? null : zone.getName());
        }
    }

    public record ZoneEventResponse(
            String id,
            String tripId,
            String zoneId,
            String zone,
            String zoneType,
            String resolutionNumber,
            String ownerName,
            String vessel,
            double latitude,
            double longitude,
            String occurredAt,
            String status) {

        public static ZoneEventResponse from(ZoneEventEntity event) {
            return new ZoneEventResponse(event.getId(), event.getTrip().getId(), event.getZone().getId(),
                    event.getZone().getName(), event.getZone().getZoneType().name(),
                    event.getZone().getResolutionNumber(), event.getOwner().getFullName(),
                    event.getTrip().getVessel().getName(), event.getLatitude().doubleValue(),
                    event.getLongitude().doubleValue(), event.getOccurredAt().toString(), event.getStatus());
        }
    }

    /** Resultado de un envío de posiciones: cuántas se guardaron y qué intrusiones hubo (AC-04.3). */
    public record PositionBatchResponse(int stored, int duplicates, List<ZoneEventResponse> intrusions) {
    }
}
