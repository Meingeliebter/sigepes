package com.sigepes.backend.common;

import java.util.UUID;

/** Generación de identificadores legibles ({@code usr-ab12cd34}). */
public final class Ids {

    private Ids() {
    }

    public static String generate(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** Devuelve {@code candidate} si viene informado; si no, genera uno nuevo. */
    public static String orGenerate(String candidate, String prefix) {
        return candidate == null || candidate.isBlank() ? generate(prefix) : candidate.trim();
    }
}
