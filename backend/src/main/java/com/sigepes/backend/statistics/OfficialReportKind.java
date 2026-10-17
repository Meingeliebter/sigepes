package com.sigepes.backend.statistics;

/** AC-18.1 · Catálogo de reportes oficiales. */
public enum OfficialReportKind {
    LANDINGS("Desembarcos por especie y zona", true, true),
    QUOTA_COMPLIANCE("Cumplimiento de cuotas", true, true),
    PERMITS("Permisos por estado", false, false),
    ILLEGAL_REPORTS("Denuncias de pesca ilegal", false, false),
    ZONE_EVENTS("Ingresos a zonas restringidas", true, false);

    private final String title;
    private final boolean regionFilter;
    private final boolean speciesFilter;

    OfficialReportKind(String title, boolean regionFilter, boolean speciesFilter) {
        this.title = title;
        this.regionFilter = regionFilter;
        this.speciesFilter = speciesFilter;
    }

    public String title() {
        return title;
    }

    /** Si el filtro de región aplica; si no, el encabezado lo dice explícitamente. */
    public boolean regionFilter() {
        return regionFilter;
    }

    public boolean speciesFilter() {
        return speciesFilter;
    }
}
