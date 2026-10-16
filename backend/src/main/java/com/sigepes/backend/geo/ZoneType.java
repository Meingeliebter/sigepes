package com.sigepes.backend.geo;

/** RF-04 · Clase de zona restringida. */
public enum ZoneType {
    /** Zona cubierta por una veda espacial. */
    CLOSURE_ZONE,
    /** Área Marina Protegida. */
    MPA,
    /** Otra restricción (seguridad marítima, zona militar, etc.). */
    OTHER
}
