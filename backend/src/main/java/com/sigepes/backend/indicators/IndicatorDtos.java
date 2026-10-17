package com.sigepes.backend.indicators;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class IndicatorDtos {

    private IndicatorDtos() {
    }

    public record Filters(LocalDate from, LocalDate to, String region, String speciesId) {
    }

    /** AC-12.3: lo que se dejó por fuera, rotulado, para que nadie lea el total como completo. */
    public record Totals(
            BigDecimal landedKg,
            long closedTrips,
            long excludedOpenTrips,
            long activeClosures,
            long openReports,
            long zoneEvents) {
    }

    public record SpeciesLanding(String speciesId, String speciesName, BigDecimal kg, long trips) {
    }

    public record RegionLanding(String region, BigDecimal kg) {
    }

    public record QuotaUsage(String id, String speciesName, String zone, BigDecimal totalKg, BigDecimal remainingKg,
            BigDecimal consumedPercent) {
    }

    public record ClosureInForce(String speciesName, String zone, LocalDate until) {
    }

    public record StatusCount(String status, long count) {
    }

    public record ZoneEventCount(String zoneName, long total, long open) {
    }

    public record IndicatorsResponse(
            Filters filters,
            /** AC-12.5: momento del cálculo; los agregados se reutilizan hasta 15 minutos. */
            Instant generatedAt,
            Totals totals,
            List<SpeciesLanding> landingsBySpecies,
            List<RegionLanding> landingsByRegion,
            List<QuotaUsage> quotas,
            List<ClosureInForce> closures,
            List<StatusCount> permitsByStatus,
            List<StatusCount> reportsByStatus,
            List<ZoneEventCount> zoneEvents) {
    }

    /** AC-12.6: lo que ve un pescador o armador, solo con sus propias faenas. */
    public record OwnIndicators(Filters filters, BigDecimal landedKg, long closedTrips, long openTrips,
            List<SpeciesLanding> landingsBySpecies) {
    }
}
