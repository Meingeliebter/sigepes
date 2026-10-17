package com.sigepes.backend.vessels;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.sigepes.backend.gears.FishingGearEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class VesselDtos {

    private VesselDtos() {
    }

    /**
     * Embarcación tal y como la consume la pantalla "Embarcaciones".
     *
     * <p>Campos alineados con la interfaz {@code Vessel} de {@code shared/api/domain.service.ts}:
     * {@code gears} son los nombres de las artes, que es lo que pintan los chips.
     */
    public record VesselResponse(
            String id,
            String name,
            String registration,
            String port,
            List<String> gears,
            List<String> gearIds,
            String status,
            String ownerId,
            String ownerName,
            String type,
            String hullType,
            BigDecimal enginePower,
            BigDecimal capacityTons) {

        public static VesselResponse from(VesselEntity entity) {
            List<FishingGearEntity> gears = entity.getGears().stream()
                    .sorted(Comparator.comparing(FishingGearEntity::getName))
                    .toList();
            return new VesselResponse(
                    entity.getId(),
                    entity.getName(),
                    entity.getRegistration(),
                    entity.getPort(),
                    gears.stream().map(FishingGearEntity::getName).toList(),
                    gears.stream().map(FishingGearEntity::getId).toList(),
                    entity.getStatus().name(),
                    entity.getOwner().getId(),
                    entity.getOwner().getFullName(),
                    entity.getVesselType(),
                    entity.getHullType(),
                    entity.getEnginePower(),
                    entity.getCapacityTons());
        }
    }

    /** Alta de embarcación. {@code gears} admite nombres o identificadores de arte. */
    public record CreateVesselRequest(
            @NotBlank(message = "El nombre de la embarcación es obligatorio")
            @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
            String name,

            @NotBlank(message = "La matrícula es obligatoria")
            @Size(min = 3, max = 60, message = "La matrícula debe tener entre 3 y 60 caracteres")
            String registration,

            @NotBlank(message = "El puerto base es obligatorio")
            @Size(max = 120, message = "El puerto no puede superar 120 caracteres")
            String port,

            List<String> gears,

            @Pattern(regexp = "ACTIVE|UNDER_MAINTENANCE|DECOMMISSIONED|SUSPENDED", message = "El estado no es válido")
            String status,

            @Size(max = 60, message = "El tipo no puede superar 60 caracteres")
            String type,

            @NotBlank(message = "El tipo de casco es obligatorio")
            @Size(max = 60, message = "El tipo de casco no puede superar 60 caracteres")
            String hullType,

            @NotNull(message = "La potencia del motor es obligatoria")
            @Positive(message = "La potencia del motor debe ser mayor que cero")
            BigDecimal enginePower,

            @NotNull(message = "La capacidad es obligatoria")
            @Positive(message = "La capacidad debe ser mayor que cero")
            BigDecimal capacityTons,

            /** Solo lo usa un administrador para registrar a nombre de otro usuario. */
            String ownerId) {
    }

    /** Actualización de estado y artes de una embarcación. */
    public record UpdateVesselRequest(
            @Pattern(regexp = "ACTIVE|UNDER_MAINTENANCE|DECOMMISSIONED|SUSPENDED", message = "El estado no es válido")
            String status,

            List<String> gears,

            @Size(max = 120, message = "El puerto no puede superar 120 caracteres")
            String port) {
    }
}
