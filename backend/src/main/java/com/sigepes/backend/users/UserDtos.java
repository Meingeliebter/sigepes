package com.sigepes.backend.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    /**
     * Usuario expuesto por la API.
     *
     * <p>Coincide con {@code User} de {@code modules/auth/types/auth.types.ts} y con la fila de la
     * tabla de la pantalla "Usuarios" (que añade {@code status}).
     */
    public record UserResponse(
            String id,
            String nationalId,
            String fullName,
            String email,
            String role,
            String status) {

        public static UserResponse from(UserEntity entity) {
            return new UserResponse(
                    entity.getId(),
                    entity.getNationalId(),
                    entity.getFullName(),
                    entity.getEmail(),
                    entity.getRole().name(),
                    entity.getStatus().name());
        }
    }

    /** Alta de usuario desde la pantalla de administración. */
    public record CreateUserRequest(
            @NotBlank(message = "La identificación es obligatoria")
            @Pattern(regexp = "\\d{6,12}", message = "La identificación debe tener entre 6 y 12 dígitos")
            String nationalId,

            @NotBlank(message = "El nombre completo es obligatorio")
            @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
            String fullName,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,

            @NotBlank(message = "El rol es obligatorio")
            @Pattern(regexp = "FISHER|SHIPOWNER|INSPECTOR|ANALYST|ADMIN", message = "El rol no es válido")
            String role,

            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
            String password) {
    }

    /** Cambio de rol y/o estado de un usuario (administración). */
    public record UpdateUserRequest(
            @Pattern(regexp = "FISHER|SHIPOWNER|INSPECTOR|ANALYST|ADMIN", message = "El rol no es válido")
            String role,

            @Pattern(regexp = "ACTIVE|SUSPENDED", message = "El estado no es válido")
            String status,

            @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
            String fullName) {
    }
}
