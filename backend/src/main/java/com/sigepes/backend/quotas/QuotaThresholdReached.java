package com.sigepes.backend.quotas;

/**
 * AC-07.4 · Evento de dominio: una cuota cruzó un umbral de consumo (80, 95 o 100 %).
 *
 * <p>Se publica dentro de la transacción del descuento; el módulo de alertas (HU-11) lo escucha
 * para notificar a analistas y armadores sin que el módulo de cuotas dependa de él.
 */
public record QuotaThresholdReached(String quotaId, String speciesName, String zone, String vesselOwnerId,
        int threshold, String consumedPercent, String remainingKg) {
}
