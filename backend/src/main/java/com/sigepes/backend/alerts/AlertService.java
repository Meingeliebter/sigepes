package com.sigepes.backend.alerts;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.alerts.AlertDtos.AlertInbox;
import com.sigepes.backend.alerts.AlertDtos.AlertResponse;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserRepository;
import com.sigepes.backend.users.UserStatus;

/**
 * RF-11 · Buzón de alertas.
 *
 * <p>Las alertas se guardan antes de enviarse: el registro en base de datos es la fuente de verdad
 * y el canal en vivo solo acelera la entrega. Un usuario sin conexión las recibe al volver
 * (AC-11.5). Cada alerta lleva una clave de origen única por destinatario, así que reprocesar el
 * mismo hecho no la duplica.
 */
@Service
public class AlertService {

    static final Set<Role> AUTHORITY = Set.of(Role.INSPECTOR, Role.ADMIN);
    private static final int MAX_INBOX = 200;

    private final AlertRepository repository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;

    public AlertService(AlertRepository repository, UserRepository userRepository, CurrentUser currentUser,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Generación (AC-11.1, AC-11.4)
    // ------------------------------------------------------------------

    /** Crea la alerta si ese destinatario no la tiene ya. Devuelve true si es nueva. */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean notify(String recipientId, AlertType type, AlertSeverity severity, String title, String message,
            String link, String sourceKey) {
        if (recipientId == null || repository.existsByRecipientIdAndSourceKey(recipientId, sourceKey)) {
            return false;
        }
        AlertEntity saved = repository.save(new AlertEntity(Ids.generate("ale"), recipientId, type, severity,
                truncate(title, 160), truncate(message, 600), link, sourceKey));
        events.publishEvent(new AlertCreated(recipientId, AlertResponse.from(saved)));
        return true;
    }

    /** AC-11.4: entrega a todos los usuarios activos con alguno de los roles indicados. */
    @Transactional(propagation = Propagation.REQUIRED)
    public int notifyRoles(Collection<Role> roles, AlertType type, AlertSeverity severity, String title,
            String message, String link, String sourceKey) {
        int created = 0;
        for (UserEntity user : activeUsersWith(roles)) {
            if (notify(user.getId(), type, severity, title, message, link, sourceKey)) {
                created++;
            }
        }
        return created;
    }

    List<UserEntity> activeUsersWith(Collection<Role> roles) {
        return userRepository.findByRoleInAndStatus(roles, UserStatus.ACTIVE);
    }

    // ------------------------------------------------------------------
    // Buzón del usuario (AC-11.2, AC-11.3)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AlertInbox inbox(int limit) {
        String userId = currentUser.require().id();
        List<AlertResponse> alerts = repository
                .findByRecipientIdAndArchivedAtIsNullOrderByCreatedAtDesc(userId,
                        Limit.of(Math.min(Math.max(limit, 1), MAX_INBOX)))
                .stream()
                .map(AlertResponse::from)
                .toList();
        return new AlertInbox(unreadCount(userId), alerts);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return unreadCount(currentUser.require().id());
    }

    private long unreadCount(String userId) {
        return repository.countByRecipientIdAndReadAtIsNullAndArchivedAtIsNull(userId);
    }

    @Transactional
    public AlertResponse markRead(String id) {
        AlertEntity alert = own(id);
        alert.markRead();
        return AlertResponse.from(repository.save(alert));
    }

    @Transactional
    public long markAllRead() {
        String userId = currentUser.require().id();
        repository.markAllRead(userId, Instant.now());
        return unreadCount(userId);
    }

    /** RN-22: una alerta crítica de cumplimiento no se puede archivar ni borrar. */
    @Transactional
    public void archive(String id) {
        AlertEntity alert = own(id);
        if (alert.getSeverity() == AlertSeverity.CRITICAL) {
            throw ApiException.of(ErrorCode.ALERT_CRITICAL_PERSISTENT);
        }
        alert.archive();
        repository.save(alert);
    }

    /** Solo el destinatario ve su alerta; para cualquier otro no existe. */
    private AlertEntity own(String id) {
        return repository.findByIdAndRecipientId(id, currentUser.require().id())
                .orElseThrow(() -> ApiException.of(ErrorCode.ALERT_NOT_FOUND));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
