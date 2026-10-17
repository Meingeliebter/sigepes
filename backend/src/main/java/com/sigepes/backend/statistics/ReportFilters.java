package com.sigepes.backend.statistics;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.indicators.Region;

/** AC-18.2 · Periodo, región y especie de un reporte oficial. */
public record ReportFilters(LocalDate from, LocalDate to, Region region, String speciesId) {

    /** Un reporte de hasta un año se genera en línea; más largo, en segundo plano (AC-18.5). */
    static final int SYNC_MAX_DAYS = 366;
    private static final int MAX_DAYS = 10 * 366;

    public static ReportFilters of(LocalDate from, LocalDate to, Region region, String speciesId) {
        LocalDate end = to == null ? AppClock.today() : to;
        LocalDate start = from == null ? end.minusMonths(12).plusDays(1) : from;
        if (start.isAfter(end)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "La fecha inicial no puede ser posterior a la final.");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "El periodo no puede superar diez años.");
        }
        return new ReportFilters(start, end, region, speciesId == null || speciesId.isBlank() ? null : speciesId);
    }

    public boolean needsBackground() {
        return ChronoUnit.DAYS.between(from, to) + 1 > SYNC_MAX_DAYS;
    }
}
