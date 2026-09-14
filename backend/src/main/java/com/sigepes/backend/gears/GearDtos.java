package com.sigepes.backend.gears;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class GearDtos {

    private GearDtos() {
    }

    public record GearResponse(
            String id,
            String name,
            String description,
            boolean destructive,
            String status) {

        public static GearResponse from(FishingGearEntity entity) {
            return new GearResponse(entity.getId(), entity.getName(), entity.getDescription(),
                    entity.isDestructive(), entity.getStatus().name());
        }
    }

    public record CreateGearRequest(
            @NotBlank(message = "El nombre del arte es obligatorio")
            @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
            String name,

            @Size(max = 400, message = "La descripción no puede superar 400 caracteres")
            String description,

            Boolean destructive,

            @Pattern(regexp = "ACTIVE|BLOCKED|INACTIVE", message = "El estado no es válido")
            String status) {
    }
}
