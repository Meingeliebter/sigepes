package com.sigepes.backend.auth;

import com.sigepes.backend.users.UserDtos.UserResponse;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthModels {

    private AuthModels() {
    }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            String password) {
    }

    /**
     * Registro público. Las reglas replican las del esquema Zod del formulario
     * ({@code modules/auth/schemas/auth.schemas.ts}); {@code confirmPassword} no se envía.
     */
    public record RegisterRequest(
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

    /** Respuesta esperada por {@code authStore}: {@code { accessToken, user }}. */
    public record Session(String accessToken, String tokenType, long expiresIn, UserResponse user) {
    }
}
