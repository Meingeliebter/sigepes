package com.sigepes.backend.documents;

/** Catálogos de la gestión documental (RF-15). */
public final class DocumentEnums {

    private DocumentEnums() {
    }

    /** AC-15.1 · Clase de documento. */
    public enum DocType {
        ID_DOCUMENT,
        VESSEL_CERTIFICATE,
        PERMIT,
        RESOLUTION,
        OTHER
    }

    /** AC-15.1 · Registro al que se vincula el documento. */
    public enum EntityType {
        USER,
        VESSEL,
        PERMIT,
        CLOSURE
    }

    /** AC-15.3 · Estado según el vencimiento de la versión vigente. */
    public enum ExpiryStatus {
        VALID,
        EXPIRING,
        EXPIRED,
        NO_EXPIRY
    }
}
