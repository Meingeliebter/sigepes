package com.sigepes.backend.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.Codes;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.FileStorageService;
import com.sigepes.backend.common.FileStorageService.StoredFile;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.reports.PhotoSanitizer.CleanPhoto;
import com.sigepes.backend.reports.ReportDtos.ChangeStatusRequest;
import com.sigepes.backend.reports.ReportDtos.ReportDetail;
import com.sigepes.backend.reports.ReportDtos.ReportReceipt;
import com.sigepes.backend.reports.ReportDtos.ReportSummary;
import com.sigepes.backend.reports.ReportDtos.ReportTrackingResponse;
import com.sigepes.backend.reports.ReportDtos.StatusChangeResponse;
import com.sigepes.backend.reports.ReportDtos.SubmitReportRequest;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserService;

/**
 * RF-10 · Denuncias de pesca ilegal.
 *
 * <p>La identidad del denunciante anónimo nunca entra al sistema (RN-20): la denuncia se guarda sin
 * usuario aunque haya sesión iniciada, la auditoría se escribe sin actor ni IP y las fotos pierden
 * sus metadatos antes de tocar el disco (AC-10.5).
 */
@Service
public class ReportService {

    static final int MAX_PHOTOS = 3;

    private final IllegalReportRepository reportRepository;
    private final ReportPhotoRepository photoRepository;
    private final ReportStatusChangeRepository changeRepository;
    private final PhotoSanitizer photoSanitizer;
    private final FileStorageService storage;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public ReportService(IllegalReportRepository reportRepository, ReportPhotoRepository photoRepository,
            ReportStatusChangeRepository changeRepository, PhotoSanitizer photoSanitizer, FileStorageService storage,
            UserService userService, CurrentUser currentUser, AuditService auditService,
            ApplicationEventPublisher events) {
        this.reportRepository = reportRepository;
        this.photoRepository = photoRepository;
        this.changeRepository = changeRepository;
        this.photoSanitizer = photoSanitizer;
        this.storage = storage;
        this.userService = userService;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Radicación (AC-10.1, AC-10.2, AC-10.3, AC-10.6)
    // ------------------------------------------------------------------

    /**
     * Radica una denuncia. Es idempotente por {@code clientId}: si la cola offline del dispositivo
     * la reenvía, se devuelve el mismo código de seguimiento en vez de crear un duplicado.
     */
    @Transactional
    public ReportReceipt submit(SubmitReportRequest request, List<MultipartFile> photos) {
        if (request.clientId() != null) {
            var existing = reportRepository.findByClientId(request.clientId());
            if (existing.isPresent()) {
                return ReportReceipt.from(existing.get());
            }
        }
        boolean hasCoordinates = request.latitude() != null && request.longitude() != null;
        boolean hasPlace = request.locationText() != null && !request.locationText().isBlank();
        if (!hasCoordinates && !hasPlace) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "Indica dónde ocurrió: marca el punto en el mapa, usa tu GPS o describe el lugar.");
        }
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "La ubicación necesita latitud y longitud.");
        }

        List<MultipartFile> files = photos == null ? List.of()
                : photos.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (files.size() > MAX_PHOTOS) {
            throw ApiException.of(ErrorCode.TOO_MANY_REPORT_PHOTOS);
        }
        // Se limpian todas antes de guardar nada: una foto inválida rechaza la denuncia completa.
        List<CleanPhoto> clean = files.stream().map(photoSanitizer::sanitize).toList();

        UserEntity reporter = request.anonymous() ? null
                : currentUser.find().map(user -> userService.byId(user.id())).orElse(null);
        IllegalReportEntity report = new IllegalReportEntity(Ids.generate("den"), newTrackingCode(),
                ReportType.valueOf(request.reportType()), request.description().trim(), request.occurredOn(),
                reporter);
        report.setClientId(request.clientId());
        report.setLocation(request.latitude(), request.longitude());
        report.setLocationText(hasPlace ? request.locationText().trim() : null);
        IllegalReportEntity saved = reportRepository.save(report);

        for (CleanPhoto photo : clean) {
            String photoId = Ids.generate("fot");
            StoredFile stored = storage.storeBytes(photo.content(), photo.contentType(), "denuncias/" + saved.getId(),
                    photoId);
            photoRepository.save(new ReportPhotoEntity(photoId, saved.getId(), stored.storagePath(),
                    stored.contentType(), stored.sizeBytes()));
        }

        String detail = "Denuncia " + saved.getTrackingCode() + " (" + saved.getReportType() + ") con "
                + clean.size() + " foto(s)";
        if (saved.isAnonymous()) {
            auditService.recordAnonymous(AuditActionType.CREATE, "REPORT_SUBMITTED", AuditService.MODULE_REPORTS,
                    "Denuncia", saved.getId(), detail + ", anónima");
        } else {
            auditService.record(AuditActionType.CREATE, "REPORT_SUBMITTED", AuditService.MODULE_REPORTS, "Denuncia",
                    saved.getId(), detail);
        }
        events.publishEvent(new ReportSubmitted(saved.getId(), saved.getTrackingCode(), saved.getReportType()));
        return ReportReceipt.from(saved);
    }

    /** AC-10.3 · Consulta pública del estado por código de seguimiento. */
    @Transactional(readOnly = true)
    public ReportTrackingResponse track(String trackingCode) {
        return reportRepository.findByTrackingCode(trackingCode.trim().toUpperCase())
                .map(ReportTrackingResponse::from)
                .orElseThrow(() -> ApiException.of(ErrorCode.REPORT_NOT_FOUND));
    }

    /** Denuncias identificadas del usuario autenticado; las anónimas no aparecen porque no hay vínculo. */
    @Transactional(readOnly = true)
    public List<ReportTrackingResponse> mine() {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.require().id()).stream()
                .map(ReportTrackingResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------
    // Gestión por la autoridad (AC-10.4)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReportSummary> queue(ReportStatus status) {
        List<IllegalReportEntity> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status);
        Map<String, Long> photoCounts = photoRepository.countByReport().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
        return reports.stream()
                .map(report -> new ReportSummary(report.getId(), report.getTrackingCode(),
                        report.getReportType().name(), report.getStatus().name(), report.getDescription(),
                        report.getOccurredOn(), report.getLatitude(), report.getLongitude(),
                        report.getLocationText(), report.isAnonymous(),
                        photoCounts.getOrDefault(report.getId(), 0L).intValue(), report.getCreatedAt(),
                        report.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportDetail detail(String id) {
        return toDetail(byId(id));
    }

    @Transactional
    public ReportDetail changeStatus(String id, ChangeStatusRequest request) {
        IllegalReportEntity report = byId(id);
        ReportStatus from = report.getStatus();
        ReportStatus to = ReportStatus.valueOf(request.status());
        if (!from.canMoveTo(to)) {
            throw ApiException.of(ErrorCode.INVALID_REPORT_TRANSITION);
        }
        String note = request.note().trim();
        UserEntity inspector = userService.byId(currentUser.require().id());
        report.setStatus(to);
        if (to.isClosed()) {
            report.setResolutionNote(note);
        }
        reportRepository.save(report);
        changeRepository.save(new ReportStatusChangeEntity(Ids.generate("dhs"), report.getId(), from, to, note,
                inspector));
        auditService.recordChange("REPORT_STATUS_CHANGED", AuditService.MODULE_REPORTS, "Denuncia", report.getId(),
                "Denuncia " + report.getTrackingCode() + ": " + from + " → " + to,
                AuditService.toJson(Map.of("status", from.name())),
                AuditService.toJson(Map.of("status", to.name(), "note", note)));
        return toDetail(report);
    }

    @Transactional(readOnly = true)
    public ReportPhotoEntity photo(String reportId, String photoId) {
        byId(reportId);
        return photoRepository.findByIdAndReportId(photoId, reportId)
                .orElseThrow(() -> ApiException.of(ErrorCode.REPORT_NOT_FOUND, "La foto no existe."));
    }

    public Resource loadPhoto(ReportPhotoEntity photo) {
        return storage.load(photo.getStoragePath());
    }

    private IllegalReportEntity byId(String id) {
        return reportRepository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.REPORT_NOT_FOUND));
    }

    private ReportDetail toDetail(IllegalReportEntity report) {
        List<String> photoIds = photoRepository.findByReportIdOrderByCreatedAtAsc(report.getId()).stream()
                .map(ReportPhotoEntity::getId)
                .toList();
        List<StatusChangeResponse> history = new ArrayList<>(changeRepository
                .findByReportIdOrderByChangedAtAsc(report.getId()).stream()
                .map(StatusChangeResponse::from)
                .toList());
        return new ReportDetail(report.getId(), report.getTrackingCode(), report.getReportType().name(),
                report.getStatus().name(), report.getDescription(), report.getOccurredOn(), report.getLatitude(),
                report.getLongitude(), report.getLocationText(), report.isAnonymous(),
                report.getReporter() == null ? null : report.getReporter().getFullName(),
                report.getResolutionNote(), photoIds, history, report.getCreatedAt(), report.getUpdatedAt());
    }

    private String newTrackingCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = Codes.random("DEN", 8);
            if (!reportRepository.existsByTrackingCode(code)) {
                return code;
            }
        }
        throw ApiException.of(ErrorCode.INTERNAL_ERROR, "No fue posible generar el código de seguimiento.");
    }
}
