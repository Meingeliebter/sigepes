package com.sigepes.backend.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.geo.ZoneEventEntity;
import com.sigepes.backend.geo.ZoneEventRepository;
import com.sigepes.backend.indicators.Region;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.quotas.QuotaEntity;
import com.sigepes.backend.quotas.QuotaRepository;
import com.sigepes.backend.quotas.QuotaStatus;
import com.sigepes.backend.reports.IllegalReportEntity;
import com.sigepes.backend.reports.IllegalReportRepository;
import com.sigepes.backend.species.SpeciesRepository;
import com.sigepes.backend.trips.CatchRepository;
import com.sigepes.backend.trips.FishingTripEntity;
import com.sigepes.backend.trips.FishingTripRepository;
import com.sigepes.backend.trips.TripStatus;

/**
 * RF-18 · Construye cada reporte oficial a partir de datos validados (RN-36).
 *
 * <p>Ningún reporte lleva datos personales (AC-18.6): ni nombres de pescadores o denunciantes, ni
 * documentos de identidad, ni correos. Las embarcaciones se identifican por matrícula y los
 * permisos por número.
 */
@Component
class OfficialReportBuilder {

    private static final ZoneId ZONE = AppClock.ZONE;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<String, String> REPORT_TYPE = Map.of(
            "CLOSURE_FISHING", "Pesca en veda", "UNDERSIZED_CATCH", "Captura bajo talla mínima",
            "PROHIBITED_GEAR", "Arte prohibido", "MPA_INTRUSION", "Pesca en AMP", "OTHER", "Otra");
    private static final Map<String, String> REPORT_STATUS = Map.of(
            "RECEIVED", "Recibida", "INVESTIGATING", "En investigación",
            "CLOSED_FOUNDED", "Cerrada · fundada", "CLOSED_UNFOUNDED", "Cerrada · no fundada");
    private static final Map<String, String> PERMIT_STATUS = Map.of(
            "APPROVED", "Vigente", "PENDING", "Pendiente", "REJECTED", "Rechazado", "SUSPENDED", "Suspendido",
            "EXPIRED", "Vencido");
    private static final Map<String, String> ZONE_TYPE = Map.of(
            "CLOSURE_ZONE", "Zona en veda", "MPA", "Área Marina Protegida", "OTHER", "Otra restricción");

    private final CatchRepository catchRepository;
    private final FishingTripRepository tripRepository;
    private final QuotaRepository quotaRepository;
    private final PermitRepository permitRepository;
    private final IllegalReportRepository reportRepository;
    private final ZoneEventRepository zoneEventRepository;
    private final SpeciesRepository speciesRepository;

    OfficialReportBuilder(CatchRepository catchRepository, FishingTripRepository tripRepository,
            QuotaRepository quotaRepository, PermitRepository permitRepository, IllegalReportRepository reportRepository,
            ZoneEventRepository zoneEventRepository, SpeciesRepository speciesRepository) {
        this.catchRepository = catchRepository;
        this.tripRepository = tripRepository;
        this.quotaRepository = quotaRepository;
        this.permitRepository = permitRepository;
        this.reportRepository = reportRepository;
        this.zoneEventRepository = zoneEventRepository;
        this.speciesRepository = speciesRepository;
    }

    @Transactional(readOnly = true)
    StatTable build(OfficialReportKind kind, ReportFilters filters, String generatedBy) {
        List<String> footnotes = new ArrayList<>();
        Table table = switch (kind) {
            case LANDINGS -> landings(filters, footnotes);
            case QUOTA_COMPLIANCE -> quotas(filters, footnotes);
            case PERMITS -> permits(footnotes);
            case ILLEGAL_REPORTS -> illegalReports(filters, footnotes);
            case ZONE_EVENTS -> zoneEvents(filters, footnotes);
        };
        return new StatTable(kind, kind.title(), table.columns(), table.rows(), describe(kind, filters), footnotes,
                generatedBy, Instant.now());
    }

    private record Table(List<String> columns, List<List<String>> rows) {
    }

    // ------------------------------------------------------------------

    private Table landings(ReportFilters filters, List<String> footnotes) {
        Map<String, BigDecimal[]> grouped = new LinkedHashMap<>();
        for (Object[] row : catchRepository.landings(filters.from(), filters.to())) {
            String speciesId = (String) row[0];
            String zone = (String) row[2];
            if (!Region.matches(zone, filters.region()) || (filters.speciesId() != null && !filters.speciesId().equals(speciesId))) {
                continue;
            }
            String key = row[1] + "\u0000" + zone;
            BigDecimal[] values = grouped.computeIfAbsent(key, ignored -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            values[0] = values[0].add((BigDecimal) row[3]);
            values[1] = values[1].add(BigDecimal.valueOf(((Number) row[4]).longValue()));
        }
        List<List<String>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal[]> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\u0000");
            rows.add(List.of(parts[0], parts[1], regionLabel(Region.of(parts[1])), kg(entry.getValue()[0]),
                    entry.getValue()[1].toPlainString()));
            total = total.add(entry.getValue()[0]);
        }
        rows.sort(Comparator.comparing((List<String> row) -> new BigDecimal(row.get(3))).reversed());
        List<FishingTripEntity> trips = tripRepository.findByTripDateBetween(filters.from(), filters.to()).stream()
                .filter(trip -> Region.matches(trip.getZone(), filters.region()))
                .toList();
        long excluded = trips.stream().filter(trip -> trip.getStatus() != TripStatus.CLOSED).count();
        footnotes.add("Total desembarcado: " + kg(total) + " kg en " + rows.size() + " combinación(es) de especie y zona.");
        footnotes.add("Registros excluidos por no estar validados (faenas abiertas o anuladas del periodo): " + excluded + ".");
        return new Table(List.of("Especie", "Zona", "Región", "Desembarcado (kg)", "Faenas"), rows);
    }

    private Table quotas(ReportFilters filters, List<String> footnotes) {
        List<List<String>> rows = new ArrayList<>();
        int inactive = 0;
        for (QuotaEntity quota : quotaRepository.findAllByOrderByPeriodStartDescCreatedAtDesc()) {
            boolean overlaps = !quota.getPeriodEnd().isBefore(filters.from()) && !quota.getPeriodStart().isAfter(filters.to());
            boolean matches = Region.matches(quota.getZone(), filters.region())
                    && (filters.speciesId() == null || quota.getSpecies().getId().equals(filters.speciesId()));
            if (!overlaps || !matches) {
                continue;
            }
            if (quota.getStatus() != QuotaStatus.ACTIVE) {
                inactive++;
                continue;
            }
            BigDecimal percent = quota.consumedPercent();
            String state = percent.compareTo(BigDecimal.valueOf(100)) >= 0 ? "Agotada"
                    : percent.compareTo(BigDecimal.valueOf(95)) >= 0 ? "Crítica"
                    : percent.compareTo(BigDecimal.valueOf(80)) >= 0 ? "Atención" : "En cumplimiento";
            rows.add(List.of(quota.getSpecies().getCommonName(), quota.getZone(),
                    DATE.format(quota.getPeriodStart()) + " a " + DATE.format(quota.getPeriodEnd()),
                    kg(quota.getTotalKg()), kg(quota.getConsumedKg()), percent.setScale(1, RoundingMode.HALF_UP).toPlainString(),
                    state));
        }
        footnotes.add("Cuotas inactivas o anuladas excluidas: " + inactive + ".");
        footnotes.add("El consumo solo incluye capturas de faenas cerradas (descuento al cierre, AC-07.2).");
        return new Table(List.of("Especie", "Zona", "Periodo", "Asignado (kg)", "Consumido (kg)", "Consumo (%)", "Estado"), rows);
    }

    private Table permits(List<String> footnotes) {
        LocalDate today = AppClock.today();
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<List<String>> rows = new ArrayList<>();
        for (PermitEntity permit : permitRepository.findAllByOrderByExpiresAtDesc()) {
            String status = PERMIT_STATUS.getOrDefault(permit.effectiveStatus(today).name(), permit.getStatus().name());
            counts.merge(status, 1, Integer::sum);
            rows.add(List.of(permit.getNumber(), permit.getType(), status, DATE.format(permit.getIssuedAt()),
                    DATE.format(permit.getExpiresAt())));
        }
        footnotes.add("Situación al " + DATE.format(today) + ": " + counts.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse("sin permisos") + ".");
        footnotes.add("No se incluyen nombres ni documentos de los titulares (protección de datos).");
        return new Table(List.of("Número", "Tipo", "Estado", "Emisión", "Vencimiento"), rows);
    }

    private Table illegalReports(ReportFilters filters, List<String> footnotes) {
        Instant from = filters.from().atStartOfDay(ZONE).toInstant();
        Instant to = filters.to().plusDays(1).atStartOfDay(ZONE).toInstant();
        List<List<String>> rows = new ArrayList<>();
        long anonymous = 0;
        for (IllegalReportEntity report : reportRepository.findAllByOrderByCreatedAtDesc()) {
            if (report.getCreatedAt().isBefore(from) || !report.getCreatedAt().isBefore(to)) {
                continue;
            }
            anonymous += report.isAnonymous() ? 1 : 0;
            rows.add(List.of(report.getTrackingCode(), REPORT_TYPE.get(report.getReportType().name()),
                    REPORT_STATUS.get(report.getStatus().name()), DATE.format(report.getOccurredOn()),
                    DATE.format(report.getCreatedAt().atZone(ZONE)), place(report), report.isAnonymous() ? "Sí" : "No"));
        }
        footnotes.add("Denuncias anónimas: " + anonymous + " de " + rows.size() + ". Ningún reporte identifica al denunciante (RN-20).");
        footnotes.add("Las coordenadas se redondean a dos decimales (≈1 km).");
        return new Table(List.of("Código", "Tipo", "Estado", "Fecha del hecho", "Radicada", "Lugar", "Anónima"), rows);
    }

    private Table zoneEvents(ReportFilters filters, List<String> footnotes) {
        Instant from = filters.from().atStartOfDay(ZONE).toInstant();
        Instant to = filters.to().plusDays(1).atStartOfDay(ZONE).toInstant();
        List<List<String>> rows = new ArrayList<>();
        long open = 0;
        for (ZoneEventEntity event : zoneEventRepository.findByOccurredAtBetween(from, to)) {
            if (!Region.matches(event.getTrip().getZone(), filters.region())) {
                continue;
            }
            boolean pending = "OPEN".equals(event.getStatus());
            open += pending ? 1 : 0;
            rows.add(List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(event.getOccurredAt().atZone(ZONE)),
                    event.getZone().getName(), ZONE_TYPE.getOrDefault(event.getZone().getZoneType().name(), "Otra"),
                    event.getTrip().getVessel().getRegistration(), pending ? "Pendiente" : "Revisado"));
        }
        rows.sort(Comparator.comparing((List<String> row) -> row.get(0)));
        footnotes.add("Eventos pendientes de revisión: " + open + " de " + rows.size() + ".");
        return new Table(List.of("Fecha y hora", "Zona", "Tipo de zona", "Embarcación (matrícula)", "Estado"), rows);
    }

    // ------------------------------------------------------------------

    private List<String> describe(OfficialReportKind kind, ReportFilters filters) {
        List<String> lines = new ArrayList<>();
        lines.add(kind == OfficialReportKind.PERMITS
                ? "Periodo: no aplica (situación actual de los permisos)"
                : "Periodo: " + DATE.format(filters.from()) + " a " + DATE.format(filters.to()));
        if (kind.regionFilter()) {
            lines.add("Región: " + (filters.region() == null ? "todas" : regionLabel(filters.region())));
        } else if (filters.region() != null) {
            lines.add("Región: no aplica a este reporte");
        }
        if (kind.speciesFilter()) {
            lines.add("Especie: " + (filters.speciesId() == null ? "todas"
                    : speciesRepository.findById(filters.speciesId()).map(s -> s.getCommonName()).orElse("desconocida")));
        } else if (filters.speciesId() != null) {
            lines.add("Especie: no aplica a este reporte");
        }
        return lines;
    }

    private static String place(IllegalReportEntity report) {
        if (report.getLatitude() != null && report.getLongitude() != null) {
            return report.getLatitude().setScale(2, RoundingMode.HALF_UP).toPlainString() + ", "
                    + report.getLongitude().setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return report.getLocationText() == null ? "" : report.getLocationText();
    }

    private static String regionLabel(Region region) {
        return switch (region) {
            case PACIFICO -> "Pacífico";
            case CARIBE -> "Caribe";
            case OTRA -> "Otra";
        };
    }

    private static String kg(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
