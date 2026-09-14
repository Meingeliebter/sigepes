package com.sigepes.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.auth.AuthModels.LoginRequest;
import com.sigepes.backend.auth.AuthModels.RegisterRequest;
import com.sigepes.backend.auth.AuthModels.Session;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-01 · Endpoints de autenticación. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve el access token")
    public Session login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un usuario nuevo (RN-01: documento único)")
    public Session register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve la sesión del usuario autenticado")
    public Session me() {
        return authService.sessionForId(currentUser.require().id());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra la sesión y deja constancia en auditoría")
    public ResponseEntity<Void> logout() {
        AuthenticatedUser user = currentUser.require();
        authService.logout(user.id(), user.fullName());
        return ResponseEntity.noContent().build();
    }
}
