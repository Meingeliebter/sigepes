package com.sigepes.backend.geo;

/**
 * RN-08 · Evento de dominio: una faena entró a una zona restringida.
 *
 * <p>El módulo de alertas (HU-11) lo escucha para avisar al pescador y a los inspectores.
 */
public record RestrictedZoneEntered(String eventId, String tripId, String ownerId, String zoneName,
        String zoneType, String vesselName, double latitude, double longitude) {
}
