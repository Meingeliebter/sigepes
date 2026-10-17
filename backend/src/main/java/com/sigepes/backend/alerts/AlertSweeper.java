package com.sigepes.backend.alerts;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditLogRepository;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.closures.ClosureStatus;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitService;
import com.sigepes.backend.users.Role;

/**
 * AC-11.1 · Alertas que dependen del calendario y no de un evento: vedas que empiezan o están
 * vigentes, permisos a 30/15/5 días de vencer e intentos de fuerza bruta.
 *
 * <p>Es idempotente: cada hecho tiene una clave de origen y repetir la revisión no duplica nada,
 * así que puede ejecutarse cada pocos minutos y también al arrancar.
 */
@Service
public class AlertSweeper {

    private static final Set<Role> OPERATORS = Set.of(Role.FISHER, Role.SHIPOWNER);
    private static final int UPCOMING_CLOSURE_DAYS = 7;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es"));
    private static final DateTimeFormatter HOUR_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final AlertService alerts;
    private final ClosureRepository closureRepository;
    private final PermitService permitService;
    private final AuditService auditService;

    public AlertSweeper(AlertService alerts, ClosureRepository closureRepository, PermitService permitService,
            AuditService auditService) {
        this.alerts = alerts;
        this.closureRepository = closureRepository;
        this.permitService = permitService;
        this.auditService = auditService;
    }

    /** Devuelve cuántas alertas nuevas se generaron. */
    @Transactional
    public int sweep() {
        LocalDate today = AppClock.today();
        return closures(today) + permits(today) + security();
    }

    private int closures(LocalDate today) {
        int created = 0;
        for (ClosureEntity closure : closureRepository.findAllByOrderByStartDateDesc()) {
            ClosureStatus status = closure.effectiveStatus(today);
            String species = closure.getSpecies().getCommonName();
            if (status == ClosureStatus.CLOSED) {
                created += alerts.notifyRoles(OPERATORS, AlertType.CLOSURE, AlertSeverity.WARNING,
                        "Veda vigente: " + species,
                        "Está prohibido capturar " + species + " en " + closure.getZone() + " hasta el "
                                + DAY.format(closure.getEndDate()) + ".",
                        "/app/closures", "closure:" + closure.getId() + ":active");
            } else if (status == ClosureStatus.SCHEDULED
                    && ChronoUnit.DAYS.between(today, closure.getStartDate()) <= UPCOMING_CLOSURE_DAYS) {
                created += alerts.notifyRoles(OPERATORS, AlertType.CLOSURE, AlertSeverity.INFO,
                        "Veda próxima: " + species,
                        "Desde el " + DAY.format(closure.getStartDate()) + " no se podrá capturar " + species
                                + " en " + closure.getZone() + ".",
                        "/app/closures", "closure:" + closure.getId() + ":upcoming");
            }
        }
        return created;
    }

    /** AC-08.2 · Un aviso por umbral (30, 15, 5 días), solo al titular del permiso. */
    private int permits(LocalDate today) {
        int created = 0;
        for (PermitEntity permit : permitService.expiringSoon(null)) {
            Integer tier = permitService.tierFor(permit, today);
            if (tier == null || permit.getOwner() == null) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, permit.getExpiresAt());
            AlertSeverity severity = tier <= 5 ? AlertSeverity.CRITICAL
                    : tier <= 15 ? AlertSeverity.WARNING : AlertSeverity.INFO;
            boolean fresh = alerts.notify(permit.getOwner().getId(), AlertType.PERMIT, severity,
                    "Permiso " + permit.getNumber() + " por vencer",
                    "Tu permiso vence en " + daysLeft + " día(s), el " + DAY.format(permit.getExpiresAt())
                            + ". Renuévalo para seguir pescando legalmente.",
                    "/app/permits", "permit:" + permit.getId() + ":" + tier);
            created += fresh ? 1 : 0;
        }
        return created;
    }

    /** AC-20.5 · Fuerza bruta: solo para ADMIN, una alerta por IP y por hora. */
    private int security() {
        int created = 0;
        String bucket = HOUR_BUCKET.format(LocalDateTime.now(AppClock.ZONE));
        for (AuditLogRepository.SuspiciousIp ip : auditService.suspiciousLoginIps()) {
            created += alerts.notifyRoles(Set.of(Role.ADMIN), AlertType.SECURITY, AlertSeverity.CRITICAL,
                    "Posible ataque de fuerza bruta",
                    ip.getFailures() + " intentos de inicio de sesión fallidos desde " + ip.getIpAddress()
                            + " en los últimos " + auditService.bruteForceWindowMinutes() + " minutos.",
                    "/app/audit", "security:" + ip.getIpAddress() + ":" + bucket);
        }
        return created;
    }
}
