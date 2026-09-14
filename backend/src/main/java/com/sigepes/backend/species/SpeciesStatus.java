package com.sigepes.backend.species;

/** Estados de especie reconocidos por la interfaz (StatusChip). */
public enum SpeciesStatus {
    /** Habilitada para captura y comercialización. */
    ACTIVE,
    /** Con veda vigente. Se recalcula a partir de las vedas (RF-05). */
    CLOSED,
    /** Retirada del catálogo. */
    INACTIVE
}
