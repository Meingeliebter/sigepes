package com.sigepes.backend.quotas;

/** RF-07 · Estado administrativo de una cuota. */
public enum QuotaStatus {
    /** Vigente: se descuentan los desembarques que caen en su periodo. */
    ACTIVE,
    /** Cerrada por la autoridad: ya no admite descuentos. */
    CLOSED
}
