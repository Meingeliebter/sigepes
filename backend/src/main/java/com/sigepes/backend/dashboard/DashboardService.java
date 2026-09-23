package com.sigepes.backend.dashboard;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditLogEntity;
import com.sigepes.backend.audit.AuditLogRepository;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.dashboard.DashboardDtos.ActivityResponse;
import com.sigepes.backend.dashboard.DashboardDtos.ComplianceResponse;
import com.sigepes.backend.dashboard.DashboardDtos.DashboardResponse;
import com.sigepes.backend.dashboard.DashboardDtos.NotificationResponse;
import com.sigepes.backend.dashboard.DashboardDtos.StatResponse;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.permits.PermitService;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.trips.FishingTripRepository;
import com.sigepes.backend.vessels.VesselRepository;
import com.sigepes.backend.vessels.VesselStatus;

/**
 * Resumen operativo de la pantalla de inicio y notificaciones.
 *
 * <p>Todo se calcula con datos del MVP v1.0 (embarcaciones, permisos, faenas y vedas): no hay aquí
 * ningún módulo de alertas propio, que corresponde a la v1.1.
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy",
            Locale.forLanguageTag("es"));
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final VesselRepository vesselRepository;
    private final PermitRepository permitRepository;
    private final FishingTripRepository tripRepository;
    private final ClosureRepository closureRepository;
    private final PermitService permitService;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public DashboardService(VesselRepository vesselRepository, PermitRepository permitRepository,
            FishingTripRepository tripRepository, ClosureRepository closureRepository, PermitService permitService,
            AuditService auditService, CurrentUser currentUser) {
        this.vesselRepository = vesselRepository;
        this.permitRepository = permitRepository;
        this.tripRepository = tripRepository;
        this.closureRepository = closureRepository;
        this.permitService = permitService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        AuthenticatedUser actor = currentUser.require();
        LocalDate today = AppClock.today();
        LocalDate monthStart = today.withDayOfMonth(1);
        boolean authority = actor.isAuthority();

        long vessels = authority ? vesselRepository.count() : vesselRepository.countByOwnerId(actor.id());
        long maintenance = authority
                ? vesselRepository.findAllByOrderByNameAsc().stream()
                        .filter(vessel -> vessel.getStatus() == VesselStatus.UNDER_MAINTENANCE).count()
                : vesselRepository.countByOwnerIdAndStatus(actor.id(), VesselStatus.UNDER_MAINTENANCE);

        List<PermitEntity> permits = authority
                ? permitRepository.findAllByOrderByExpiresAtDesc()
                : permitRepository.findByOwnerIdOrderByExpiresAtDesc(actor.id());
        long validPermits = permits.stream().filter(permit -> permit.isValidOn(today)).count();
        long expiringPermits = permits.stream()
                .filter(permit -> permit.isExpiringSoon(today, permitService.warningDays())).count();
        long pendingPermits = permits.stream().filter(permit -> permit.getStatus() == PermitStatus.PENDING).count();

        long tripsThisMonth = authority
                ? tripRepository.countByTripDateGreaterThanEqual(monthStart)
                : tripRepository.countByOwnerIdAndTripDateGreaterThanEqual(actor.id(), monthStart);

        List<ClosureEntity> closures = closureRepository.findInForceOn(today);
        long warnings = closures.size() + expiringPermits;

        List<String> alerts = new ArrayList<>();
        if (expiringPermits > 0) {
            alerts.add("Tienes " + expiringPermits + " permiso(s) próximo(s) a vencer.");
        }
        if (pendingPermits > 0) {
            alerts.add(pendingPermits + " permiso(s) en estado pendiente de revisión.");
        }
        if (!closures.isEmpty()) {
            alerts.add(closures.size() + " veda(s) vigente(s) en este momento.");
        }
        // AC-20.5: incidente de seguridad, visible solo para ADMIN (no toda "autoridad").
        if (actor.isAdmin()) {
            List<AuditLogRepository.SuspiciousIp> suspicious = auditService.suspiciousLoginIps();
            if (!suspicious.isEmpty()) {
                alerts.add(suspicious.size() + " IP(s) con actividad sospechosa de inicio de sesión.");
            }
        }
        if (alerts.isEmpty()) {
            alerts.add("Sin alertas de cumplimiento en este momento.");
        }

        return new DashboardResponse(
                actor.fullName(),
                alerts,
                new StatResponse(vessels, maintenance + " en mantenimiento"),
                new StatResponse(validPermits, expiringPermits + " por vencer"),
                new StatResponse(tripsThisMonth, "registradas este mes"),
                new StatResponse(warnings, closures.size() + " veda(s) y " + expiringPermits + " permiso(s)"),
                recentActivity(authority ? null : actor.id()),
                compliance(today, validPermits, permits.size(), closures));
    }

    /** Notificaciones derivadas de vedas vigentes y permisos por vencer. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> notifications() {
        AuthenticatedUser actor = currentUser.require();
        LocalDate today = AppClock.today();
        List<NotificationResponse> notifications = new ArrayList<>();

        for (ClosureEntity closure : closureRepository.findInForceOn(today)) {
            notifications.add(new NotificationResponse(
                    "closure-" + closure.getId(),
                    "CLOSURE",
                    "Veda vigente: " + closure.getSpecies().getCommonName(),
                    "Restricción en " + closure.getZone() + " hasta el " + DAY.format(closure.getEndDate()) + ".",
                    DAY.format(closure.getStartDate()),
                    "WARNING"));
        }

        // AC-08.2: un permiso puede cruzar varios umbrales a la vez (p. ej. 30 y 15); solo se avisa
        // con el más urgente para no duplicar la alerta.
        for (PermitEntity permit : permitService.expiringSoon(actor.isAuthority() ? null : actor.id())) {
            Integer tier = permitService.tierFor(permit, today);
            if (tier == null) {
                continue;
            }
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, permit.getExpiresAt());
            notifications.add(new NotificationResponse(
                    "permit-" + permit.getId(),
                    "PERMIT",
                    "Permiso próximo a vencer",
                    "El permiso " + permit.getNumber() + " vence en " + daysLeft + " día(s) (el "
                            + DAY.format(permit.getExpiresAt()) + ") — alerta de " + tier + " días.",
                    DAY.format(today),
                    severityForTier(tier)));
        }

        // AC-20.5: alerta de seguridad por IP con fallos de login recientes, solo visible para ADMIN.
        if (actor.isAdmin()) {
            for (AuditLogRepository.SuspiciousIp ip : auditService.suspiciousLoginIps()) {
                notifications.add(new NotificationResponse(
                        "security-" + ip.getIpAddress(),
                        "INFO",
                        "Posible ataque de fuerza bruta",
                        ip.getFailures() + " intentos de inicio de sesión fallidos desde " + ip.getIpAddress()
                                + " en los últimos " + auditService.bruteForceWindowMinutes() + " minutos.",
                        DAY.format(today),
                        "CRITICAL"));
            }
        }

        if (notifications.isEmpty()) {
            notifications.add(new NotificationResponse("empty", "INFO", "Sin novedades",
                    "No hay vedas vigentes ni permisos próximos a vencer.", DAY.format(today), "INFO"));
        }
        return notifications;
    }

    /** AC-08.2: 5 días es crítico, 15 es advertencia, 30 (o cualquier umbral mayor) es informativo. */
    private static String severityForTier(int tier) {
        if (tier <= 5) {
            return "CRITICAL";
        }
        return tier <= 15 ? "WARNING" : "INFO";
    }

    private List<ActivityResponse> recentActivity(String ownerId) {
        return auditService.search(null, null, null, null, null, 40).stream()
                .filter(entry -> ownerId == null || ownerId.equals(entry.getActorId()))
                .filter(entry -> !"LOGIN".equals(entry.getAction()))
                .limit(6)
                .map(DashboardService::toActivity)
                .toList();
    }

    private static ActivityResponse toActivity(AuditLogEntity entry) {
        String status = switch (entry.getAction()) {
            case "TRIP_CLOSED", "PERMIT_APPROVED", "TRIP_SYNCHRONIZED" -> "APPROVED";
            case "PERMIT_REJECTED", "PERMIT_SUSPENDED" -> "REJECTED";
            case "CLOSURE_CREATED", "CLOSURE_UPDATED" -> "CLOSED";
            case "TRIP_STARTED", "PERMIT_REQUESTED" -> "PENDING";
            default -> "INFO";
        };
        String title = entry.getDetail() == null || entry.getDetail().isBlank()
                ? entry.getEntity() + " · " + entry.getAction()
                : entry.getDetail();
        return new ActivityResponse(entry.getId(), title, status,
                DAY.format(entry.getOccurredAt().atZone(ZONE).toLocalDate()));
    }

    private static List<ComplianceResponse> compliance(LocalDate today, long validPermits, int totalPermits,
            List<ClosureEntity> closures) {
        List<ComplianceResponse> rows = new ArrayList<>();
        rows.add(new ComplianceResponse("Permisos al día",
                totalPermits == 0 ? "PENDING" : (validPermits > 0 ? "VALID" : "EXPIRED")));
        rows.add(new ComplianceResponse("Tallas mínimas", "ACTIVE"));
        rows.add(new ComplianceResponse(
                closures.isEmpty() ? "Vedas vigentes" : "Veda de " + closures.get(0).getSpecies().getCommonName(),
                closures.isEmpty() ? "OPEN" : "CLOSED"));
        rows.add(new ComplianceResponse("Bitácora al " + DAY.format(today), "ACTIVE"));
        return rows;
    }
}
