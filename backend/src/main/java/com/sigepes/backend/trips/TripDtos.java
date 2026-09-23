package com.sigepes.backend.trips;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class TripDtos {

    private TripDtos() {
    }

    /** Captura tal y como la consume la interfaz ({@code Capture}). */
    public record CatchResponse(
            String id,
            String speciesId,
            String species,
            Integer quantity,
            BigDecimal weightKg,
            BigDecimal averageSizeCm,
            BigDecimal minSizeCmAtCapture) {

        public static CatchResponse from(CatchEntity entity) {
            return new CatchResponse(entity.getId(), entity.getSpecies().getId(),
                    entity.getSpecies().getCommonName(), entity.getQuantity(), entity.getWeightKg(),
                    entity.getAverageSizeCm(), entity.getMinSizeCmAtCapture());
        }
    }

    /**
     * Faena tal y como la consume la tabla de la bitácora.
     *
     * <p>{@code vessel} lleva el nombre de la embarcación porque es lo que pinta la tabla, y
     * {@code date} va en ISO {@code yyyy-MM-dd}, igual que lo envía el formulario.
     */
    public record TripResponse(
            String id,
            String vesselId,
            String vessel,
            String zone,
            String originPort,
            String date,
            String status,
            boolean nullDeclaration,
            List<CatchResponse> captures,
            String ownerId,
            String ownerName,
            String gearId,
            String gear,
            String permitNumber,
            String departureTime,
            String arrivalTime,
            BigDecimal landingLatitude,
            BigDecimal landingLongitude,
            String notes,
            BigDecimal totalWeightKg) {

        public static TripResponse from(FishingTripEntity entity) {
            List<CatchResponse> captures = entity.getCatches().stream().map(CatchResponse::from).toList();
            BigDecimal total = captures.stream()
                    .map(CatchResponse::weightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new TripResponse(
                    entity.getId(),
                    entity.getVessel().getId(),
                    entity.getVessel().getName(),
                    entity.getZone(),
                    entity.getOriginPort(),
                    entity.getTripDate().toString(),
                    entity.getStatus().name(),
                    entity.isNullDeclaration(),
                    captures,
                    entity.getOwner().getId(),
                    entity.getOwner().getFullName(),
                    entity.getGear() == null ? null : entity.getGear().getId(),
                    entity.getGear() == null ? null : entity.getGear().getName(),
                    entity.getPermit() == null ? null : entity.getPermit().getNumber(),
                    entity.getDepartureTime() == null ? null : entity.getDepartureTime().toString(),
                    entity.getArrivalTime() == null ? null : entity.getArrivalTime().toString(),
                    entity.getLandingLatitude(),
                    entity.getLandingLongitude(),
                    entity.getNotes(),
                    total);
        }
    }

    /**
     * Apertura de una faena.
     *
     * <p>{@code id} es opcional: el cliente puede proponer uno (lo hace para poder sincronizar un
     * registro creado sin conexión sin duplicarlo).
     */
    public record CreateTripRequest(
            String id,

            @NotBlank(message = "La embarcación es obligatoria")
            String vesselId,

            String gearId,

            @NotBlank(message = "La zona de faena es obligatoria")
            @Size(max = 120, message = "La zona no puede superar 120 caracteres")
            String zone,

            @NotBlank(message = "El puerto de origen es obligatorio")
            @Size(max = 120, message = "El puerto de origen no puede superar 120 caracteres")
            String originPort,

            LocalDate date,

            LocalTime departureTime,

            LocalTime arrivalTime,

            @Size(max = 600, message = "Las observaciones no pueden superar 600 caracteres")
            String notes,

            /** Se ignora salvo que quien llama sea administrador. */
            String ownerId) {
    }

    /** Registro de una captura dentro de una faena abierta. */
    public record CreateCatchRequest(
            String id,

            @NotBlank(message = "La especie es obligatoria")
            String speciesId,

            @NotNull(message = "El conteo individual es obligatorio")
            @Positive(message = "La cantidad debe ser mayor que cero")
            Integer quantity,

            @NotNull(message = "El peso es obligatorio")
            @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero")
            BigDecimal weightKg,

            @NotNull(message = "La talla promedio es obligatoria")
            @DecimalMin(value = "0.01", message = "La talla promedio debe ser mayor que cero")
            BigDecimal averageSizeCm) {
    }

    /**
     * Cierre de faena (con capturas o con declaración de captura nula, RN-06) y también cuerpo de
     * la corrección posterior ({@code PATCH /faenas/{id}}).
     *
     * <p>{@code landingLatitude}/{@code landingLongitude} son obligatorias al cerrar (AC-03.2,
     * validado en {@code TripService.close}, no aquí, porque este mismo record lo reutiliza
     * {@code correct} donde no aplican). {@code reason} solo se exige al corregir una faena ya
     * cerrada (AC-03.5, validado en {@code TripService.correct}).
     */
    public record CloseTripRequest(
            Boolean nullDeclaration,

            LocalTime arrivalTime,

            @Size(max = 600, message = "Las observaciones no pueden superar 600 caracteres")
            String notes,

            BigDecimal landingLatitude,

            BigDecimal landingLongitude,

            @Size(max = 400, message = "El motivo no puede superar 400 caracteres")
            String reason) {
    }

    /** Faena pendiente enviada por el cliente al recuperar conexión. */
    public record SyncTripRequest(
            String id,
            String vesselId,
            String zone,
            String originPort,
            LocalDate date,
            String status,
            Boolean nullDeclaration,
            List<CreateCatchRequest> captures,
            BigDecimal landingLatitude,
            BigDecimal landingLongitude,
            String ownerId) {
    }
}
