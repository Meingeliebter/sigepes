package com.sigepes.backend.audit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    private static final int MAX_DETAIL = 600;
    private static final int MAX_STATE = 2000;

    private final AuditLogRepository repository;
    private final CurrentUser currentUser;

    public AuditService(AuditLogRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    /** Registra un evento atribuido al usuario autenticado. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String module, String entity, String entityId, String detail) {
        AuthenticatedUser actor = currentUser.find().orElse(null);
        recordAs(actor == null ? null : actor.id(), actor == null ? "sistema" : actor.fullName(),
                action, module, entity, entityId, detail, null, null);
    }

    /** Registra un evento con estado anterior y posterior (correcciones, cambios sensibles). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordChange(String action, String module, String entity, String entityId, String detail,
            String beforeState, String afterState) {
        AuthenticatedUser actor = currentUser.find().orElse(null);
        recordAs(actor == null ? null : actor.id(), actor == null ? "sistema" : actor.fullName(),
                action, module, entity, entityId, detail, beforeState, afterState);
    }

    /**
     * Registra un evento atribuido explícitamente a un actor. Necesario en login/registro, donde
     * todavía no hay contexto de seguridad establecido.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordAs(String actorId, String actorLabel, String action, String module, String entity,
            String entityId, String detail, String beforeState, String afterState) {
        AuditLogEntity entry = new AuditLogEntity(
                Ids.generate("aud"),
                Instant.now(),
                actorId,
                actorLabel == null || actorLabel.isBlank() ? "sistema" : truncate(actorLabel, 160),
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

    @Transactional(readOnly = true)
    public List<AuditLogEntity> latest(String module, int limit) {
        Limit cap = Limit.of(Math.min(Math.max(limit, 1), 500));
        return module == null || module.isBlank()
                ? repository.findAllByOrderByOccurredAtDesc(cap)
                : repository.findByModuleOrderByOccurredAtDesc(module, cap);
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
