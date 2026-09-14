package com.sigepes.backend.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;

/** Acceso al usuario autenticado desde los servicios de dominio. */
@Component
public class CurrentUser {

    public Optional<AuthenticatedUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user ? Optional.of(user) : Optional.empty();
    }

    /** Devuelve el usuario autenticado o lanza {@code UNAUTHORIZED}. */
    public AuthenticatedUser require() {
        return find().orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHORIZED));
    }
}
