package com.sigepes.backend.documents;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.FileSignatures;
import com.sigepes.backend.common.FileStorageService;
import com.sigepes.backend.common.FileStorageService.StoredFile;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.documents.DocumentDtos.CreateDocumentRequest;
import com.sigepes.backend.documents.DocumentDtos.DocumentResponse;
import com.sigepes.backend.documents.DocumentDtos.DownloadLink;
import com.sigepes.backend.documents.DocumentDtos.NewVersionRequest;
import com.sigepes.backend.documents.DocumentDtos.VersionResponse;
import com.sigepes.backend.documents.DocumentEnums.DocType;
import com.sigepes.backend.documents.DocumentEnums.EntityType;
import com.sigepes.backend.documents.DocumentEnums.ExpiryStatus;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserService;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselRepository;

/**
 * RF-15 · Gestión documental versionada.
 *
 * <p>Cada carga se valida por el contenido real del archivo y su tamaño, se guarda en el disco
 * persistente con su hash SHA-256 y crea una versión nueva sin tocar las anteriores (RN-30). Leer
 * un documento está permitido al titular, a inspectores y a administradores, y la descarga solo se
 * hace con un enlace firmado de pocos minutos. Cargas, rechazos y descargas quedan auditados.
 */
@Service
public class DocumentService {

    static final long MAX_BYTES = 5L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final FileStorageService storage;
    private final DownloadLinkSigner signer;
    private final UserService userService;
    private final VesselRepository vesselRepository;
    private final PermitRepository permitRepository;
    private final ClosureRepository closureRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public DocumentService(DocumentRepository documentRepository, DocumentVersionRepository versionRepository,
            FileStorageService storage, DownloadLinkSigner signer, UserService userService,
            VesselRepository vesselRepository, PermitRepository permitRepository, ClosureRepository closureRepository,
            CurrentUser currentUser, AuditService auditService) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.storage = storage;
        this.signer = signer;
        this.userService = userService;
        this.vesselRepository = vesselRepository;
        this.permitRepository = permitRepository;
        this.closureRepository = closureRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------
    // Consulta (AC-15.4)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(DocType type, EntityType entityType, ExpiryStatus status) {
        AuthenticatedUser actor = currentUser.require();
        LocalDate today = AppClock.today();
        List<DocumentEntity> documents = canReadAll(actor)
                ? documentRepository.findAllByOrderByUpdatedAtDesc()
                : documentRepository.findByOwnerIdOrderByUpdatedAtDesc(actor.id());
        return documents.stream()
                .filter(document -> type == null || document.getDocType() == type)
                .filter(document -> entityType == null || document.getEntityType() == entityType)
                .filter(document -> status == null || document.expiryStatus(today) == status)
                .map(document -> toResponse(document, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse detail(String id) {
        return toResponse(readable(id), true);
    }

    // ------------------------------------------------------------------
    // Carga y versiones (AC-15.1, AC-15.2, AC-15.5)
    // ------------------------------------------------------------------

    @Transactional
    public DocumentResponse create(CreateDocumentRequest request, MultipartFile file) {
        AuthenticatedUser actor = currentUser.require();
        EntityType entityType = EntityType.valueOf(request.entityType());
        UserEntity owner = linkedOwner(entityType, request.entityId(), actor);
        Upload upload = validate(file, actor);

        DocumentEntity document = documentRepository.save(new DocumentEntity(Ids.generate("doc"), owner,
                DocType.valueOf(request.docType()), request.title().trim(), entityType, request.entityId()));
        addVersion(document, upload, request.expiresOn(), request.note(), actor);
        auditService.record(AuditActionType.CREATE, "DOCUMENT_UPLOADED", AuditService.MODULE_DOCUMENTS, "Documento",
                document.getId(), "«" + document.getTitle() + "» (" + document.getDocType() + ") v1, "
                        + upload.contentType() + ", " + upload.bytes().length + " bytes");
        return toResponse(document, true);
    }

    @Transactional
    public DocumentResponse newVersion(String id, NewVersionRequest request, MultipartFile file) {
        AuthenticatedUser actor = currentUser.require();
        DocumentEntity document = readable(id);
        if (!document.getOwner().getId().equals(actor.id()) && !canReadAll(actor)) {
            throw ApiException.of(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        Upload upload = validate(file, actor);
        int version = addVersion(document, upload, request.expiresOn(), request.note(), actor);
        auditService.record(AuditActionType.UPDATE, "DOCUMENT_VERSION_ADDED", AuditService.MODULE_DOCUMENTS,
                "Documento", document.getId(), "«" + document.getTitle() + "» v" + version + " (las anteriores se conservan)");
        return toResponse(document, true);
    }

    private int addVersion(DocumentEntity document, Upload upload, LocalDate expiresOn, String note,
            AuthenticatedUser actor) {
        int version = document.getCurrentVersion() + 1;
        String versionId = Ids.generate("ver");
        StoredFile stored = storage.storeBytes(upload.bytes(), upload.contentType(), "documentos/" + document.getId(),
                versionId);
        versionRepository.save(new DocumentVersionEntity(versionId, document.getId(), version, stored.storagePath(),
                upload.filename(), upload.contentType(), upload.bytes().length, sha256(upload.bytes()), expiresOn,
                note == null || note.isBlank() ? null : note.trim(), actor.id()));
        document.advanceTo(version, expiresOn);
        documentRepository.save(document);
        return version;
    }

    private record Upload(byte[] bytes, String contentType, String filename) {
    }

    /**
     * AC-15.5 · Tipo por firma real y tamaño. Un rechazo se audita en una transacción propia para que
     * el intento quede registrado aunque la carga se revierta.
     */
    private Upload validate(MultipartFile file, AuthenticatedUser actor) {
        if (file == null || file.isEmpty()) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Adjunta el archivo del documento.");
        }
        if (file.getSize() > MAX_BYTES) {
            rejected(actor, file, "supera 5 MB");
            throw ApiException.of(ErrorCode.INVALID_DOCUMENT_FILE, "El archivo supera el máximo de 5 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.of(ErrorCode.INVALID_DOCUMENT_FILE);
        }
        String contentType = FileSignatures.detect(bytes);
        if (contentType == null) {
            rejected(actor, file, "el contenido no es PDF, JPG ni PNG (declarado: " + file.getContentType() + ")");
            throw ApiException.of(ErrorCode.INVALID_DOCUMENT_FILE);
        }
        return new Upload(bytes, contentType, safeFilename(file.getOriginalFilename(), contentType));
    }

    private void rejected(AuthenticatedUser actor, MultipartFile file, String reason) {
        auditService.recordIsolated(actor.id(), actor.fullName(), AuditActionType.SECURITY, "DOCUMENT_REJECTED",
                AuditService.MODULE_DOCUMENTS, "Documento", null,
                "Carga rechazada de «" + safeFilename(file.getOriginalFilename(), null) + "»: " + reason);
    }

    // ------------------------------------------------------------------
    // Descarga con enlace de corta duración (AC-15.5)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DownloadLink link(String id, int versionNo) {
        DocumentEntity document = readable(id);
        DocumentVersionEntity version = versionRepository.findByDocumentIdAndVersionNo(document.getId(), versionNo)
                .orElseThrow(() -> ApiException.of(ErrorCode.DOCUMENT_NOT_FOUND, "La versión no existe."));
        Instant expiresAt = Instant.now().plus(DownloadLinkSigner.TTL);
        String token = signer.sign(version.getId(), currentUser.require().id(), expiresAt);
        return new DownloadLink("/api/v1/documentos/descarga/" + token, expiresAt);
    }

    public record Download(DocumentVersionEntity version, Resource resource) {
    }

    @Transactional
    public Download download(String token) {
        DownloadLinkSigner.Grant grant = signer.verify(token);
        if (grant == null) {
            throw ApiException.of(ErrorCode.DOCUMENT_LINK_EXPIRED);
        }
        DocumentVersionEntity version = versionRepository.findById(grant.versionId())
                .orElseThrow(() -> ApiException.of(ErrorCode.DOCUMENT_NOT_FOUND));
        UserEntity user = userService.byId(grant.userId());
        DocumentEntity document = documentRepository.findById(version.getDocumentId())
                .orElseThrow(() -> ApiException.of(ErrorCode.DOCUMENT_NOT_FOUND));
        // El permiso se revalida al descargar: si al usuario le quitaron el acceso, el enlace ya no sirve.
        if (!document.getOwner().getId().equals(user.getId()) && !canReadAll(user.getRole())) {
            throw ApiException.of(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        auditService.recordAs(user.getId(), user.getFullName(), AuditActionType.SECURITY, "DOCUMENT_DOWNLOADED",
                AuditService.MODULE_DOCUMENTS, "Documento", document.getId(),
                "Descarga de «" + document.getTitle() + "» v" + version.getVersionNo(), null, null);
        return new Download(version, storage.load(version.getStoragePath()));
    }

    // ------------------------------------------------------------------

    /** El documento queda a nombre del titular del registro vinculado, para que siempre pueda verlo. */
    private UserEntity linkedOwner(EntityType type, String entityId, AuthenticatedUser actor) {
        boolean authority = canReadAll(actor);
        UserEntity owner = switch (type) {
            case USER -> {
                if (!authority && !entityId.equals(actor.id())) {
                    throw ApiException.of(ErrorCode.FORBIDDEN, "Solo puedes cargar documentos a tu nombre.");
                }
                yield userService.byId(entityId);
            }
            case VESSEL -> {
                VesselEntity vessel = vesselRepository.findById(entityId)
                        .orElseThrow(() -> ApiException.of(ErrorCode.VESSEL_NOT_FOUND));
                yield vessel.getOwner();
            }
            case PERMIT -> {
                PermitEntity permit = permitRepository.findById(entityId)
                        .orElseThrow(() -> ApiException.of(ErrorCode.PERMIT_NOT_FOUND));
                yield permit.getOwner();
            }
            case CLOSURE -> {
                if (!authority) {
                    throw ApiException.of(ErrorCode.FORBIDDEN, "Solo la autoridad carga resoluciones de veda.");
                }
                closureRepository.findById(entityId).orElseThrow(() -> ApiException.of(ErrorCode.CLOSURE_NOT_FOUND));
                yield userService.byId(actor.id());
            }
        };
        if (!authority && !owner.getId().equals(actor.id())) {
            throw ApiException.of(ErrorCode.FORBIDDEN, "El registro pertenece a otro usuario.");
        }
        return owner;
    }

    private DocumentEntity readable(String id) {
        DocumentEntity document = documentRepository.findById(id)
                .orElseThrow(() -> ApiException.of(ErrorCode.DOCUMENT_NOT_FOUND));
        AuthenticatedUser actor = currentUser.require();
        if (!document.getOwner().getId().equals(actor.id()) && !canReadAll(actor)) {
            throw ApiException.of(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    /** AC-15.5: además del titular, solo inspectores y administradores. */
    private static boolean canReadAll(AuthenticatedUser actor) {
        return canReadAll(actor.role());
    }

    private static boolean canReadAll(Role role) {
        return role == Role.INSPECTOR || role == Role.ADMIN;
    }

    private DocumentResponse toResponse(DocumentEntity document, boolean withVersions) {
        List<VersionResponse> versions = withVersions
                ? versionRepository.findByDocumentIdOrderByVersionNoDesc(document.getId()).stream()
                        .map(VersionResponse::from).toList()
                : List.of();
        return new DocumentResponse(document.getId(), document.getDocType().name(), document.getTitle(),
                document.getEntityType().name(), document.getEntityId(), entityLabel(document),
                document.getOwner().getFullName(), document.getCurrentVersion(), document.getExpiresOn(),
                document.expiryStatus(AppClock.today()).name(), document.getUpdatedAt(), versions);
    }

    private String entityLabel(DocumentEntity document) {
        return switch (document.getEntityType()) {
            case USER -> "Titular: " + document.getOwner().getFullName();
            case VESSEL -> vesselRepository.findById(document.getEntityId())
                    .map(vessel -> "Embarcación " + vessel.getName() + " (" + vessel.getRegistration() + ")")
                    .orElse("Embarcación");
            case PERMIT -> permitRepository.findById(document.getEntityId())
                    .map(permit -> "Permiso " + permit.getNumber()).orElse("Permiso");
            case CLOSURE -> closureRepository.findById(document.getEntityId())
                    .map(ClosureEntity::getResolutionNumber).map(number -> "Veda · " + number).orElse("Veda");
        };
    }

    /** Solo el nombre, sin rutas ni caracteres raros; nunca se usa para guardar en disco. */
    private static String safeFilename(String original, String contentType) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N} ._()-]", "_").trim();
        if (name.isBlank()) {
            name = "documento" + (FileSignatures.PDF.equals(contentType) ? ".pdf" : "");
        }
        return name.length() <= 120 ? name : name.substring(name.length() - 120);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
