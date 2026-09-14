package com.sigepes.backend.closures;

/**
 * Estado de una veda.
 *
 * <p>Se usan estos nombres porque son los que sabe interpretar el chip de estado de la interfaz:
 * una veda vigente se pinta en rojo como «Cerrada».
 */
public enum ClosureStatus {
    /** Programada: aún no ha comenzado. */
    SCHEDULED,
    /** Vigente: la veda está en curso. */
    CLOSED,
    /** Terminada o levantada por la autoridad. */
    FINISHED
}
