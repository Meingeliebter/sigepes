package com.sigepes.backend.indicators;

import java.text.Normalizer;
import java.util.Locale;

/** AC-12.2 · Región litoral a partir del nombre de zona ("Pacífico Norte", "Caribe", "TODAS"). */
public enum Region {
    PACIFICO,
    CARIBE,
    OTRA;

    public static Region of(String zone) {
        String normalized = normalize(zone);
        if (normalized.contains("pacific")) {
            return PACIFICO;
        }
        if (normalized.contains("carib")) {
            return CARIBE;
        }
        return OTRA;
    }

    /** Una zona "TODAS" (cuotas, vedas nacionales) aplica a cualquier región. */
    public static boolean matches(String zone, Region filter) {
        return filter == null || "todas".equals(normalize(zone)) || of(zone) == filter;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }
}
