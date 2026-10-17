package com.sigepes.backend.statistics;

import java.time.Instant;
import java.util.List;

/**
 * Reporte ya calculado, independiente del formato: lo mismo se muestra en pantalla, se exporta a
 * CSV y se imprime en PDF (AC-18.3).
 *
 * @param filters    filtros en lenguaje natural, para el encabezado.
 * @param footnotes  RN-36: registros excluidos y aclaraciones, siempre visibles al pie.
 */
public record StatTable(
        OfficialReportKind kind,
        String title,
        List<String> columns,
        List<List<String>> rows,
        List<String> filters,
        List<String> footnotes,
        String generatedBy,
        Instant generatedAt) {
}
