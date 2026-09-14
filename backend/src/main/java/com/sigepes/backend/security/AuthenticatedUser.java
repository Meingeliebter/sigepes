package com.sigepes.backend.security;

import com.sigepes.backend.users.Role;

/** Principal autenticado que viaja en el {@code SecurityContext}. */
public record AuthenticatedUser(String id, String email, String fullName, Role role) {

    public boolean isAuthority() {
        return role.isAuthority();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
