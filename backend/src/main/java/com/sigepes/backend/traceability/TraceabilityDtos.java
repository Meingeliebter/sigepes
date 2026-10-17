package com.sigepes.backend.traceability;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TraceabilityDtos {

    private TraceabilityDtos() {
    }

    /** RN-18 · Una verificación de origen legal y su resultado. */
    public record LegalCheck(String key, String label, boolean passed, String detail) {
    }

    /** Captura de una faena cerrada que se puede convertir en lote. */
    public record EligibleCatch(
            String catchId,
            String speciesName,
            BigDecimal weightKg,
            BigDecimal availableKg,
            boolean legal,
            List<LegalCheck> checks) {
    }

    public record CreateLotRequest(
            @NotBlank(message = "La captura es obligatoria")
            String catchId,

            @NotNull(message = "El peso es obligatorio")
            @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero")
            @Digits(integer = 10, fraction = 2, message = "El peso admite máximo dos decimales")
            BigDecimal weightKg) {
    }

    public record TransferRequest(
            @NotBlank(message = "Indica a quién se entrega el lote")
            @Pattern(regexp = "BUYER|TRADER|PROCESSOR|EXPORTER", message = "El tipo de receptor no es válido")
            String partyType,

            @NotBlank(message = "El nombre del receptor es obligatorio")
            @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
            String partyName,

            @Size(max = 30, message = "El NIT no puede superar 30 caracteres")
            String partyTaxId,

            @NotBlank(message = "El lugar es obligatorio")
            @Size(max = 120, message = "El lugar no puede superar 120 caracteres")
            String location,

            @PastOrPresent(message = "La fecha no puede estar en el futuro")
            LocalDate occurredOn) {
    }

    public record ChainEventResponse(
            int sequence,
            String type,
            String partyType,
            String partyName,
            String partyTaxId,
            String location,
            LocalDate occurredOn,
            String hash) {

        static ChainEventResponse from(ChainEventEntity event) {
            return new ChainEventResponse(event.getSequenceNo(), event.getEventType().name(),
                    event.getPartyType().name(), event.getPartyName(), event.getPartyTaxId(), event.getLocation(),
                    event.getOccurredOn(), event.getHash());
        }
    }

    /** Vista completa para el titular y la autoridad. */
    public record LotResponse(
            String id,
            String code,
            String tripId,
            String speciesName,
            String scientificName,
            BigDecimal weightKg,
            LocalDate landingDate,
            String zone,
            String vesselName,
            String vesselRegistration,
            String ownerName,
            String currentHolder,
            boolean integrityOk,
            List<LegalCheck> legalChecks,
            List<ChainEventResponse> chain,
            Instant createdAt) {
    }

    /** Un eslabón visto por el público: tipo de actor, lugar y fecha; sin nombres ni NIT. */
    public record PublicChainEvent(int sequence, String type, String partyType, String location, LocalDate occurredOn) {
    }

    /**
     * AC-09.4 · Página pública del QR. Sin datos personales del pescador (Ley 1581 de 2012): ni nombre,
     * ni documento, ni ubicación exacta del desembarco.
     */
    public record PublicLotResponse(
            String code,
            String speciesName,
            String scientificName,
            BigDecimal weightKg,
            LocalDate landingDate,
            String zone,
            String vesselRegistration,
            boolean legal,
            boolean integrityOk,
            List<LegalCheck> legalChecks,
            List<PublicChainEvent> chain) {
    }
}
