package com.sigepes.backend.permits;

/** Ciclo de vida de un permiso de pesca (RF-08). */
public enum PermitStatus {
    /** Solicitado, pendiente de revisión. */
    PENDING,
    /** Aprobado por la autoridad. */
    APPROVED,
    /** Rechazado con motivo. */
    REJECTED,
    /** Suspendido con motivo. */
    SUSPENDED,
    /** Vigencia terminada. */
    EXPIRED
}
