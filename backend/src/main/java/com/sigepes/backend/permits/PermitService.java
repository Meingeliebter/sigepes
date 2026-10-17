package com.sigepes.backend.permits;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.FileStorageService;
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
    private final PermitAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    /** AC-08.2: umbrales de alerta de vencimiento, ordenados de mayor a menor (p. ej. 30, 15, 5). */
    private final int[] warningTiers;

    public PermitService(PermitRepository repository, PermitAttachmentRepository attachmentRepository,
            FileStorageService fileStorageService, UserService userService, CurrentUser currentUser,
            AuditService auditService,
            @Value("${sigepes.rules.permit-expiry-warning-days:30,15,5}") String warningTiers) {
        this.repository = repository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.warningTiers = Arrays.stream(warningTiers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .mapToInt(Integer::intValue)
                .toArray();
    }

    /** Ventana máxima de aviso (la más laxa), usada para el indicador "por vencer" de la tabla. */
    public int warningDays() {
        return warningTiers.length == 0 ? 30 : warningTiers[0];
    }

    /** La autoridad ve todos los permisos; un operador solo los suyos. */
    @Transactional(readOnly = true)
    public List<PermitResponse> findVisible() {
        AuthenticatedUser user = currentUser.require();
        LocalDate today = AppClock.today();
        List<PermitEntity> permits = user.isAuthority()
                ? repository.findAllByOrderByExpiresAtDesc()
                : repository.findByOwnerIdOrderByExpiresAtDesc(user.id());
        return permits.stream().map(permit -> toResponse(permit, today)).toList();
    }

    @Transactional(readOnly = true)
    public List<PermitEntity> findByOwner(String ownerId) {
        return repository.findByOwnerIdOrderByExpiresAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public PermitEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.PERMIT_NOT_FOUND));
    }

    /** Verificación de la constancia por número de permiso o por código de verificación (QR, AC-08.4). */
    @Transactional(readOnly = true)
    public PermitResponse validateByNumber(String numberOrCode) {
        String value = numberOrCode.trim();
        PermitEntity permit = repository.findByNumberIgnoreCase(value)
                .or(() -> repository.findByVerificationCode(value))
                .orElseThrow(() -> ApiException.of(ErrorCode.PERMIT_NOT_FOUND));
        return toResponse(permit, AppClock.today());
    }

    @Transactional
    public PermitResponse request(CreatePermitRequest request, MultipartFile idDocument,
            MultipartFile vesselOwnershipDocument) {
        AuthenticatedUser actor = currentUser.require();

        // Solo un administrador puede solicitar a nombre de otro usuario.
        String ownerId = request.ownerId() != null && !request.ownerId().isBlank() && actor.isAdmin()
                ? request.ownerId()
                : actor.id();
        UserEntity owner = userService.byId(ownerId);

        LocalDate issued = request.issued() != null ? request.issued() : AppClock.today();
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

        // AC-08.5: la copia de cédula y el certificado de propiedad son obligatorios para solicitar.
        if (idDocument == null || idDocument.isEmpty() || vesselOwnershipDocument == null
                || vesselOwnershipDocument.isEmpty()) {
            throw ApiException.of(ErrorCode.PERMIT_ATTACHMENTS_REQUIRED);
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
        storeAttachment(saved.getId(), AttachmentKind.ID_COPY, idDocument);
        storeAttachment(saved.getId(), AttachmentKind.VESSEL_OWNERSHIP, vesselOwnershipDocument);

        auditService.record(AuditActionType.CREATE, "PERMIT_REQUESTED", AuditService.MODULE_PERMITS, "Permiso",
                saved.getId(), "Solicitud del permiso " + saved.getNumber() + " (" + saved.getType() + ") para "
                        + owner.getFullName());
        return toResponse(saved);
    }

    private void storeAttachment(String permitId, AttachmentKind kind, MultipartFile file) {
        String attachmentId = Ids.generate("att");
        FileStorageService.StoredFile stored = fileStorageService.store(file, "permits/" + permitId, attachmentId);
        attachmentRepository.save(new PermitAttachmentEntity(attachmentId, permitId, kind, stored.originalFilename(),
                stored.contentType(), stored.sizeBytes(), stored.storagePath()));
    }

    /** AC-08.5: descarga un adjunto; solo el titular del permiso o la autoridad pueden acceder. */
    @Transactional(readOnly = true)
    public PermitAttachmentEntity attachment(String permitId, AttachmentKind kind) {
        PermitEntity permit = byId(permitId);
        AuthenticatedUser actor = currentUser.require();
        if (!actor.isAuthority() && !permit.getOwner().getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "El adjunto pertenece al permiso de otro usuario.");
        }
        return attachmentRepository.findByPermitIdAndKind(permitId, kind)
                .orElseThrow(() -> ApiException.of(ErrorCode.PERMIT_ATTACHMENT_NOT_FOUND));
    }

    public Resource loadAttachment(PermitAttachmentEntity attachment) {
        return fileStorageService.load(attachment.getStoragePath());
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

    /** Permisos aprobados que vencen dentro de la ventana de aviso más amplia. */
    @Transactional(readOnly = true)
    public List<PermitEntity> expiringSoon(String ownerId) {
        LocalDate today = AppClock.today();
        List<PermitEntity> source = ownerId == null
                ? repository.findAllByOrderByExpiresAtDesc()
                : repository.findByOwnerIdOrderByExpiresAtDesc(ownerId);
        return source.stream().filter(permit -> permit.isExpiringSoon(today, warningDays())).toList();
    }

    /** AC-08.2: el umbral (30/15/5) más urgente cruzado por cada permiso próximo a vencer. */
    public Integer tierFor(PermitEntity permit, LocalDate today) {
        return permit.mostUrgentTier(today, warningTiers);
    }

    public PermitResponse toResponse(PermitEntity permit) {
        return toResponse(permit, AppClock.today());
    }

    private PermitResponse toResponse(PermitEntity permit, LocalDate today) {
        return PermitResponse.from(permit, today, warningDays(), attachmentRepository.findByPermitId(permit.getId()));
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
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("status", permit.getStatus().name());
        fields.put("issued", permit.getIssuedAt().toString());
        fields.put("expires", permit.getExpiresAt().toString());
        fields.put("reason", permit.getReason());
        return AuditService.toJson(fields);
    }
}
