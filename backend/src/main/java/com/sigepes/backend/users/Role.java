package com.sigepes.backend.users;

/**
 * Roles del sistema, derivados del mapa de actores de SIGEPES.
 *
 * <p>La interfaz actual solo ofrece FISHER / INSPECTOR / ADMIN en el registro, pero el backend
 * soporta además SHIPOWNER (armador) y ANALYST (analista de sostenibilidad), que aparecen en el
 * modelo RBAC del frontend y en la matriz de requerimientos.
 */
public enum Role {
    /** Pescador artesanal. */
    FISHER,
    /** Armador / propietario industrial. */
    SHIPOWNER,
    /** Inspector AUNAP. */
    INSPECTOR,
    /** Analista de sostenibilidad. */
    ANALYST,
    /** Administrador del sistema. */
    ADMIN;

    /** Roles que operan embarcaciones y registran faenas. */
    public boolean isOperator() {
        return this == FISHER || this == SHIPOWNER;
    }

    /** Roles con potestad de control (autoridad). */
    public boolean isAuthority() {
        return this == INSPECTOR || this == ANALYST || this == ADMIN;
    }
}
