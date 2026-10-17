package com.sigepes.backend.indicators;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.geo.ZoneEventEntity;
import com.sigepes.backend.geo.ZoneEventRepository;
import com.sigepes.backend.indicators.IndicatorDtos.ClosureInForce;
import com.sigepes.backend.indicators.IndicatorDtos.Filters;
import com.sigepes.backend.indicators.IndicatorDtos.IndicatorsResponse;
import com.sigepes.backend.indicators.IndicatorDtos.OwnIndicators;
import com.sigepes.backend.indicators.IndicatorDtos.QuotaUsage;
import com.sigepes.backend.indicators.IndicatorDtos.RegionLanding;
import com.sigepes.backend.indicators.IndicatorDtos.SpeciesLanding;
import com.sigepes.backend.indicators.IndicatorDtos.StatusCount;
import com.sigepes.backend.indicators.IndicatorDtos.Totals;
import com.sigepes.backend.indicators.IndicatorDtos.ZoneEventCount;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.quotas.QuotaEntity;
import com.sigepes.backend.quotas.QuotaRepository;
import com.sigepes.backend.quotas.QuotaStatus;
import com.sigepes.backend.reports.IllegalReportRepository;
import com.sigepes.backend.reports.ReportStatus;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.trips.CatchRepository;
import com.sigepes.backend.trips.FishingTripEntity;
import com.sigepes.backend.trips.FishingTripRepository;
import com.sigepes.backend.trips.TripStatus;

/**
 * RF-12 · Tablero de indicadores de sostenibilidad.
 *
 * <p>Solo cuenta registros validados: faenas cerradas y sincronizadas. Las faenas abiertas del
 * periodo se informan aparte como excluidas (RN-24, AC-12.3). Los agregados se calculan con
 * consultas agrupadas en la base de datos y se reutilizan hasta 15 minutos por combinación de
 * filtros (AC-12.5).
 */
@Service
public class IndicatorService {

    static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final int MAX_RANGE_DAYS = 3 * 366;
    private static final ZoneId ZONE = AppClock.ZONE;

    private final CatchRepository catchRepository;
    private final FishingTripRepository tripRepository;
    private final QuotaRepository quotaRepository;
    private final ClosureRepository closureRepository;
    private final PermitRepository permitRepository;
    private final IllegalReportRepository reportRepository;
    private final ZoneEventRepository zoneEventRepository;
    private final CurrentUser currentUser;
    private final Map<Filters, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(IndicatorsResponse value, Instant expiresAt) {
    }

    public IndicatorService(CatchRepository catchRepository, FishingTripRepository tripRepository,
            QuotaRepository quotaRepository, ClosureRepository closureRepository, PermitRepository permitRepository,
            IllegalReportRepository reportRepository, ZoneEventRepository zoneEventRepository,
            CurrentUser currentUser) {
        this.catchRepository = catchRepository;
        this.tripRepository = tripRepository;
        this.quotaRepository = quotaRepository;
        this.closureRepository = closureRepository;
        this.permitRepository = permitRepository;
        this.reportRepository = reportRepository;
        this.zoneEventRepository = zoneEventRepository;
        this.currentUser = currentUser;
    }

    /** Normaliza los filtros: por defecto los últimos 12 meses, todas las regiones y especies. */
    public static Filters filters(LocalDate from, LocalDate to, Region region, String speciesId) {
        LocalDate end = to == null ? AppClock.today() : to;
        LocalDate start = from == null ? end.minusMonths(12).plusDays(1) : from;
        if (start.isAfter(end)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "La fecha inicial no puede ser posterior a la final.");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "El periodo no puede superar tres años.");
        }
        return new Filters(start, end, region == null ? null : region.name(),
                speciesId == null || speciesId.isBlank() ? null : speciesId);
    }

    @Transactional(readOnly = true)
    public IndicatorsResponse indicators(Filters filters) {
        Cached cached = cache.get(filters);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.value();
        }
        IndicatorsResponse response = compute(filters);
        if (cache.size() > 200) {
            cache.clear();
        }
        cache.put(filters, new Cached(response, Instant.now().plus(CACHE_TTL)));
        return response;
    }

    /** Vacía los agregados en memoria (pruebas y recargas manuales). */
    public void evictAll() {
        cache.clear();
    }

    /** AC-12.6 · Un pescador o armador solo ve sus propias cifras. */
    @Transactional(readOnly = true)
    public OwnIndicators own(Filters filters) {
        String ownerId = currentUser.require().id();
        Region region = regionOf(filters);
        List<SpeciesLanding> bySpecies = landings(catchRepository.landingsOf(ownerId, filters.from(), filters.to()),
                region, filters.speciesId(), null);
        List<FishingTripEntity> trips = tripRepository.findByOwnerIdOrderByTripDateDescCreatedAtDesc(ownerId).stream()
                .filter(trip -> !trip.getTripDate().isBefore(filters.from()) && !trip.getTripDate().isAfter(filters.to()))
                .filter(trip -> Region.matches(trip.getZone(), region))
                .toList();
        return new OwnIndicators(filters, sum(bySpecies),
                trips.stream().filter(trip -> trip.getStatus() == TripStatus.CLOSED).count(),
                trips.stream().filter(FishingTripEntity::isOpen).count(), bySpecies);
    }

    private IndicatorsResponse compute(Filters filters) {
        Region region = regionOf(filters);
        LocalDate today = AppClock.today();
        Map<Region, BigDecimal> byRegion = new EnumMap<>(Region.class);
        List<SpeciesLanding> bySpecies = landings(catchRepository.landings(filters.from(), filters.to()), region,
                filters.speciesId(), byRegion);

        List<FishingTripEntity> trips = tripRepository.findByTripDateBetween(filters.from(), filters.to()).stream()
                .filter(trip -> Region.matches(trip.getZone(), region))
                .toList();
        long closedTrips = trips.stream().filter(trip -> trip.getStatus() == TripStatus.CLOSED).count();
        long openTrips = trips.stream().filter(FishingTripEntity::isOpen).count();

        List<QuotaUsage> quotas = quotaRepository.findAllByOrderByPeriodStartDescCreatedAtDesc().stream()
                .filter(quota -> quota.getStatus() == QuotaStatus.ACTIVE)
                .filter(quota -> !quota.getPeriodEnd().isBefore(filters.from())
                        && !quota.getPeriodStart().isAfter(filters.to()))
                .filter(quota -> Region.matches(quota.getZone(), region))
                .filter(quota -> filters.speciesId() == null || quota.getSpecies().getId().equals(filters.speciesId()))
                .sorted(Comparator.comparing(QuotaEntity::consumedPercent).reversed())
                .map(quota -> new QuotaUsage(quota.getId(), quota.getSpecies().getCommonName(), quota.getZone(),
                        quota.getTotalKg(), quota.remainingKg(), quota.consumedPercent()))
                .toList();

        List<ClosureInForce> closures = closureRepository.findInForceOn(today).stream()
                .filter(closure -> Region.matches(closure.getZone(), region))
                .filter(closure -> filters.speciesId() == null
                        || closure.getSpecies().getId().equals(filters.speciesId()))
                .sorted(Comparator.comparing(ClosureEntity::getEndDate))
                .map(closure -> new ClosureInForce(closure.getSpecies().getCommonName(), closure.getZone(),
                        closure.getEndDate()))
                .toList();

        Map<String, Long> permits = new LinkedHashMap<>();
        for (PermitEntity permit : permitRepository.findAllByOrderByExpiresAtDesc()) {
            permits.merge(permit.effectiveStatus(today).name(), 1L, Long::sum);
        }

        Instant fromInstant = filters.from().atStartOfDay(ZONE).toInstant();
        Instant toInstant = filters.to().plusDays(1).atStartOfDay(ZONE).toInstant();
        Map<String, Long> reports = new LinkedHashMap<>();
        for (ReportStatus status : ReportStatus.values()) {
            reports.put(status.name(), 0L);
        }
        for (Object[] row : reportRepository.countByStatusBetween(fromInstant, toInstant)) {
            reports.put(((ReportStatus) row[0]).name(), (Long) row[1]);
        }
        long openReports = reports.get(ReportStatus.RECEIVED.name()) + reports.get(ReportStatus.INVESTIGATING.name());

        Map<String, long[]> events = new LinkedHashMap<>();
        for (ZoneEventEntity event : zoneEventRepository.findByOccurredAtBetween(fromInstant, toInstant)) {
            if (!Region.matches(event.getTrip().getZone(), region)) {
                continue;
            }
            long[] counts = events.computeIfAbsent(event.getZone().getName(), ignored -> new long[2]);
            counts[0]++;
            if ("OPEN".equals(event.getStatus())) {
                counts[1]++;
            }
        }
        List<ZoneEventCount> zoneEvents = events.entrySet().stream()
                .map(entry -> new ZoneEventCount(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .sorted(Comparator.comparingLong(ZoneEventCount::total).reversed())
                .toList();

        Totals totals = new Totals(sum(bySpecies), closedTrips, openTrips, closures.size(), openReports,
                zoneEvents.stream().mapToLong(ZoneEventCount::total).sum());
        return new IndicatorsResponse(filters, Instant.now(), totals, bySpecies,
                byRegion.entrySet().stream().map(entry -> new RegionLanding(entry.getKey().name(), entry.getValue()))
                        .toList(),
                quotas, closures, toCounts(permits), toCounts(reports), zoneEvents);
    }

    /** Filas {@code [speciesId, nombre, zona, kg, faenas]} agregadas por especie, con filtro de región. */
    private static List<SpeciesLanding> landings(List<Object[]> rows, Region region, String speciesId,
            Map<Region, BigDecimal> byRegion) {
        Map<String, SpeciesLanding> bySpecies = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String id = (String) row[0];
            String zone = (String) row[2];
            if (!Region.matches(zone, region) || (speciesId != null && !speciesId.equals(id))) {
                continue;
            }
            BigDecimal kg = (BigDecimal) row[3];
            long trips = ((Number) row[4]).longValue();
            bySpecies.merge(id, new SpeciesLanding(id, (String) row[1], kg, trips),
                    (a, b) -> new SpeciesLanding(id, a.speciesName(), a.kg().add(b.kg()), a.trips() + b.trips()));
            if (byRegion != null) {
                byRegion.merge(Region.of(zone), kg, BigDecimal::add);
            }
        }
        List<SpeciesLanding> result = new ArrayList<>(bySpecies.values());
        result.sort(Comparator.comparing(SpeciesLanding::kg).reversed());
        return result;
    }

    private static BigDecimal sum(List<SpeciesLanding> landings) {
        return landings.stream().map(SpeciesLanding::kg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<StatusCount> toCounts(Map<String, Long> counts) {
        return counts.entrySet().stream().map(entry -> new StatusCount(entry.getKey(), entry.getValue())).toList();
    }

    private static Region regionOf(Filters filters) {
        return filters.region() == null ? null : Region.valueOf(filters.region());
    }
}
