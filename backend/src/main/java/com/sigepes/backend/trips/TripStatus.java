package com.sigepes.backend.trips;

/** Estados de una faena (RF-03). */
public enum TripStatus {
    /** Iniciada sin confirmar (reservado para el guardado offline). */
    DRAFT,
    /** En curso: admite capturas. */
    IN_PROGRESS,
    /** Cerrada: con capturas o con declaración de captura nula. */
    CLOSED,
    /** Anulada. */
    CANCELLED
}
