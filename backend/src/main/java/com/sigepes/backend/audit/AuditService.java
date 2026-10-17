package com.sigepes.backend.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Servicio de auditoría (RF-20 / RN-40).
 *
 * <p>Toda operación sensible del sistema pasa por aquí. El registro es append-only: no existe
 * ninguna operación de modificación o borrado.
 */
@Service
public class AuditService {

    /** Módulos auditados. */
    public static final String MODULE_AUTH = "autenticacion";
    public static final String MODULE_USERS = "usuarios";
    public static final String MODULE_VESSELS = "embarcaciones";
    public static final String MODULE_GEARS = "artes-pesca";
    public static final String MODULE_SPECIES = "especies";
    public static final String MODULE_CLOSURES = "vedas";
    public static final String MODULE_PERMITS = "permisos";
    public static final String MODULE_TRIPS = "faenas";
    public static final String MODULE_QUOTAS = "cuotas";
    public static final String MODULE_GEO = "geolocalizacion";

    private static final int MAX_DETAIL = 600;
    private static final int MAX_STATE = 2000;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AuditLogRepository repository;
    private final CurrentUser currentUser;
    private final int bruteForceWindowMinutes;
    private final long bruteForceThreshold;

    public AuditService(AuditLogRepository repository, CurrentUser currentUser,
            @Value("${sigepes.security.brute-force-window-minutes:15}") int bruteForceWindowMinutes,
            @Value("${sigepes.security.brute-force-threshold:5}") long bruteForceThreshold) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.bruteForceWindowMinutes = bruteForceWindowMinutes;
        this.bruteForceThreshold = bruteForceThreshold;
    }

    /** Registra un evento (CREATE/UPDATE/DELETE) atribuido al usuario autenticado. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(AuditActionType actionType, String action, String module, String entity, String entityId,
            String detail) {
        AuthenticatedUser actor = currentUser.find().orElse(null);
        recordAs(actor == null ? null : actor.id(), actor == null ? "sistema" : actor.fullName(), actionType, action,
                module, entity, entityId, detail, null, null);
    }

    /** Registra una actualización (UPDATE) con estado anterior y posterior. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordChange(String action, String module, String entity, String entityId, String detail,
            String beforeState, String afterState) {
        AuthenticatedUser actor = currentUser.find().orElse(null);
        recordAs(actor == null ? null : actor.id(), actor == null ? "sistema" : actor.fullName(),
                AuditActionType.UPDATE, action, module, entity, entityId, detail, beforeState, afterState);
    }

    /**
     * Registra un evento atribuido explícitamente a un actor. Necesario en login/registro, donde
     * todavía no hay contexto de seguridad establecido.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordAs(String actorId, String actorLabel, AuditActionType actionType, String action,
            String module, String entity, String entityId, String detail, String beforeState, String afterState) {
        AuditLogEntity entry = new AuditLogEntity(
                Ids.generate("aud"),
                Instant.now(),
                actorId,
                actorLabel == null || actorLabel.isBlank() ? "sistema" : truncate(actorLabel, 160),
                actionType,
                action,
                module,
                entity,
                entityId,
                truncate(detail, MAX_DETAIL),
                clientIp(),
                truncate(beforeState, MAX_STATE),
                truncate(afterState, MAX_STATE));
        repository.save(entry);
    }

    /** AC-20.4: consulta filtrada por módulo, tipo de acción, actor y rango de fechas. */
    @Transactional(readOnly = true)
    public List<AuditLogEntity> search(String module, AuditActionType actionType, String actorId, Instant from,
            Instant to, int limit) {
        Limit cap = Limit.of(Math.min(Math.max(limit, 1), 500));
        return repository.search(blankToNull(module), actionType, blankToNull(actorId), from, to, cap);
    }

    /**
     * AC-20.5: IPs con una tasa inusualmente alta de intentos de inicio de sesión fallidos en la
     * ventana reciente configurada — señal clásica de fuerza bruta.
     */
    @Transactional(readOnly = true)
    public List<AuditLogRepository.SuspiciousIp> suspiciousLoginIps() {
        Instant since = Instant.now().minus(bruteForceWindowMinutes, ChronoUnit.MINUTES);
        return repository.findSuspiciousLoginIps(since, bruteForceThreshold);
    }

    public int bruteForceWindowMinutes() {
        return bruteForceWindowMinutes;
    }

    /** AC-20.2: serializa un estado (antes/después) a JSON real, no a texto libre armado a mano. */
    public static String toJson(Map<String, Object> fields) {
        try {
            return JSON.writeValueAsString(fields);
        } catch (Exception e) {
            return String.valueOf(fields);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private static String clientIp() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(AuditService::resolveIp)
                .orElse(null);
    }

    private static String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
