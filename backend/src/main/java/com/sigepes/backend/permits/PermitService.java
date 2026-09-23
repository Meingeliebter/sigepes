package com.sigepes.backend.permits;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.permits.PermitDtos.CreatePermitRequest;
import com.sigepes.backend.permits.PermitDtos.EvaluatePermitRequest;
import com.sigepes.backend.permits.PermitDtos.PermitResponse;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserService;

/** RF-08 · Ciclo de vida de permisos y licencias. */
@Service
public class PermitService {

    private final PermitRepository repository;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final int warningDays;

    public PermitService(PermitRepository repository, UserService userService, CurrentUser currentUser,
            AuditService auditService,
            @Value("${sigepes.rules.permit-expiry-warning-days:30}") int warningDays) {
        this.repository = repository;
        this.userService = userService;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.warningDays = warningDays;
    }

    public int warningDays() {
        return warningDays;
    }

    /** La autoridad ve todos los permisos; un operador solo los suyos. */
    @Transactional(readOnly = true)
    public List<PermitResponse> findVisible() {
        AuthenticatedUser user = currentUser.require();
        LocalDate today = LocalDate.now();
        List<PermitEntity> permits = user.isAuthority()
                ? repository.findAllByOrderByExpiresAtDesc()
                : repository.findByOwnerIdOrderByExpiresAtDesc(user.id());
        return permits.stream().map(permit -> PermitResponse.from(permit, today, warningDays)).toList();
    }

    @Transactional(readOnly = true)
    public List<PermitEntity> findByOwner(String ownerId) {
        return repository.findByOwnerIdOrderByExpiresAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public PermitEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.PERMIT_NOT_FOUND));
    }

    /** Verificación pública de la constancia por número de permiso. */
    @Transactional(readOnly = true)
    public PermitResponse validateByNumber(String number) {
        PermitEntity permit = repository.findByNumberIgnoreCase(number.trim())
                .orElseThrow(() -> ApiException.of(ErrorCode.PERMIT_NOT_FOUND));
        return PermitResponse.from(permit, LocalDate.now(), warningDays);
    }

    @Transactional
    public PermitResponse request(CreatePermitRequest request) {
        AuthenticatedUser actor = currentUser.require();

        // Solo un administrador puede solicitar a nombre de otro usuario.
        String ownerId = request.ownerId() != null && !request.ownerId().isBlank() && actor.isAdmin()
                ? request.ownerId()
                : actor.id();
        UserEntity owner = userService.byId(ownerId);

        LocalDate issued = request.issued() != null ? request.issued() : LocalDate.now();
        LocalDate expires = request.expires() != null ? request.expires() : issued.plusYears(1);
        if (expires.isBefore(issued)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "La fecha de vencimiento no puede ser anterior a la de emisión.");
        }

        String number = request.number() == null || request.number().isBlank()
                ? nextNumber()
                : request.number().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByNumberIgnoreCase(number)) {
            throw ApiException.of(ErrorCode.PERMIT_ALREADY_REGISTERED);
        }

        // El estado solo puede fijarlo la autoridad; una solicitud de un operador nace PENDING.
        PermitStatus status = PermitStatus.PENDING;
        if (request.status() != null && !request.status().isBlank() && actor.isAuthority()) {
            status = PermitStatus.valueOf(request.status());
        }

        PermitEntity permit = new PermitEntity(
                Ids.generate("per"),
                number,
                request.type().trim(),
                request.holder() == null || request.holder().isBlank() ? owner.getFullName() : request.holder().trim(),
                owner,
                issued,
                expires,
                status,
                verificationCode(number));

        PermitEntity saved = repository.save(permit);
        auditService.record(AuditActionType.CREATE, "PERMIT_REQUESTED", AuditService.MODULE_PERMITS, "Permiso", saved.getId(),
                "Solicitud del permiso " + saved.getNumber() + " (" + saved.getType() + ") para "
                        + owner.getFullName());
        return toResponse(saved);
    }

    /**
     * Resolución de la autoridad. Rechazo y suspensión exigen motivo, y todo cambio de estado queda
     * auditado con el valor anterior y el nuevo.
     */
    @Transactional
    public PermitResponse evaluate(String id, EvaluatePermitRequest request) {
        AuthenticatedUser actor = currentUser.require();
        PermitStatus target = PermitStatus.valueOf(request.status());

        if ((target == PermitStatus.REJECTED || target == PermitStatus.SUSPENDED)
                && (request.reason() == null || request.reason().isBlank())) {
            throw ApiException.of(ErrorCode.PERMIT_REASON_REQUIRED);
        }

        PermitEntity permit = byId(id);
        String before = describe(permit);

        if (request.issued() != null) {
            permit.setIssuedAt(request.issued());
        }
        if (request.expires() != null) {
            permit.setExpiresAt(request.expires());
        }
        if (permit.getExpiresAt().isBefore(permit.getIssuedAt())) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "La fecha de vencimiento no puede ser anterior a la de emisión.");
        }

        permit.setStatus(target);
        permit.setReason(request.reason());
        permit.setReviewedBy(userService.byId(actor.id()));
        permit.setReviewedAt(Instant.now());

        PermitEntity saved = repository.save(permit);
        auditService.recordChange(auditAction(target), AuditService.MODULE_PERMITS, "Permiso", saved.getId(),
                "Permiso " + saved.getNumber() + " → " + target
                        + (request.reason() == null || request.reason().isBlank() ? "" : ". Motivo: "
                                + request.reason()),
                before, describe(saved));
        return toResponse(saved);
    }

    /** Permisos aprobados que vencen dentro de la ventana de aviso. */
    @Transactional(readOnly = true)
    public List<PermitEntity> expiringSoon(String ownerId) {
        LocalDate today = LocalDate.now();
        List<PermitEntity> source = ownerId == null
                ? repository.findAllByOrderByExpiresAtDesc()
                : repository.findByOwnerIdOrderByExpiresAtDesc(ownerId);
        return source.stream().filter(permit -> permit.isExpiringSoon(today, warningDays)).toList();
    }

    public PermitResponse toResponse(PermitEntity permit) {
        return PermitResponse.from(permit, LocalDate.now(), warningDays);
    }

    private String nextNumber() {
        String candidate;
        do {
            candidate = "PES-" + String.format("%04d", (int) (Math.random() * 10000));
        } while (repository.existsByNumberIgnoreCase(candidate));
        return candidate;
    }

    private static String verificationCode(String number) {
        return "SIGEPES-" + number.replace("-", "") + "-" + Ids.generate("v").substring(2, 8).toUpperCase(Locale.ROOT);
    }

    private static String auditAction(PermitStatus status) {
        return switch (status) {
            case APPROVED -> "PERMIT_APPROVED";
            case REJECTED -> "PERMIT_REJECTED";
            case SUSPENDED -> "PERMIT_SUSPENDED";
            default -> "PERMIT_UPDATED";
        };
    }

    private static String describe(PermitEntity permit) {
        return "status=" + permit.getStatus() + "; issued=" + permit.getIssuedAt() + "; expires="
                + permit.getExpiresAt() + "; reason=" + permit.getReason();
    }
}
