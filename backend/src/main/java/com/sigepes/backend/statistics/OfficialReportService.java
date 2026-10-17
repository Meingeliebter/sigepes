package com.sigepes.backend.statistics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.FileStorageService;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;

/**
 * RF-18 · Reportes y estadísticas oficiales.
 *
 * <p>Un reporte de hasta un año se genera en la misma petición (AC-18.5: menos de 10 s). Uno más
 * largo se encola y se genera en segundo plano, para que la API siga respondiendo; el usuario
 * recibe una alerta cuando está listo. Toda exportación queda auditada (AC-18.6).
 */
@Service
public class OfficialReportService {

    static final int PREVIEW_ROWS = 200;

    private final OfficialReportBuilder builder;
    private final ReportRenderer renderer;
    private final ExportJobRepository jobRepository;
    private final ExportJobRunner runner;
    private final FileStorageService storage;
    private final TaskExecutor executor;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public OfficialReportService(OfficialReportBuilder builder, ReportRenderer renderer,
            ExportJobRepository jobRepository, ExportJobRunner runner, FileStorageService storage,
            @Qualifier("exportExecutor") TaskExecutor executor, CurrentUser currentUser, AuditService auditService) {
        this.builder = builder;
        this.renderer = renderer;
        this.jobRepository = jobRepository;
        this.runner = runner;
        this.storage = storage;
        this.executor = executor;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    public record CatalogEntry(String kind, String title, boolean regionFilter, boolean speciesFilter) {
    }

    public record Preview(StatTable table, int totalRows, boolean truncated, boolean background) {
    }

    public record JobResponse(String id, String kind, String title, String format, LocalDate from, LocalDate to,
            String status, Integer rowCount, String error, Instant createdAt, Instant finishedAt) {

        static JobResponse from(ExportJobEntity job) {
            return new JobResponse(job.getId(), job.getKind().name(), job.getKind().title(), job.getFormat().name(),
                    job.getPeriodFrom(), job.getPeriodTo(), job.getStatus().name(), job.getRowCount(), job.getError(),
                    job.getCreatedAt(), job.getFinishedAt());
        }
    }

    /** Resultado de exportar: el archivo listo o el trabajo encolado. */
    public record ExportResult(byte[] content, String filename, String contentType, JobResponse job) {
    }

    public record JobFile(Resource resource, String filename, String contentType) {
    }

    public List<CatalogEntry> catalog() {
        return Arrays.stream(OfficialReportKind.values())
                .map(kind -> new CatalogEntry(kind.name(), kind.title(), kind.regionFilter(), kind.speciesFilter()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Preview preview(OfficialReportKind kind, ReportFilters filters) {
        StatTable table = builder.build(kind, filters, currentUser.require().fullName());
        List<List<String>> rows = table.rows();
        StatTable limited = new StatTable(table.kind(), table.title(), table.columns(),
                rows.subList(0, Math.min(rows.size(), PREVIEW_ROWS)), table.filters(), table.footnotes(),
                table.generatedBy(), table.generatedAt());
        return new Preview(limited, rows.size(), rows.size() > PREVIEW_ROWS, filters.needsBackground());
    }

    @Transactional
    public ExportResult export(OfficialReportKind kind, ExportJobEntity.Format format, ReportFilters filters) {
        AuthenticatedUser actor = currentUser.require();
        if (filters.needsBackground()) {
            ExportJobEntity job = jobRepository.save(new ExportJobEntity(Ids.generate("exp"), actor.id(), kind, format, filters));
            auditService.record(AuditActionType.CREATE, "REPORT_EXPORT_QUEUED", AuditService.MODULE_STATISTICS,
                    "Reporte oficial", job.getId(), kind + " " + format + " de " + filters.from() + " a " + filters.to());
            submitAfterCommit(job.getId());
            return new ExportResult(null, null, null, JobResponse.from(job));
        }
        StatTable table = builder.build(kind, filters, actor.fullName());
        byte[] content = format == ExportJobEntity.Format.PDF ? renderer.pdf(table) : renderer.csv(table);
        auditService.record(AuditActionType.SECURITY, "REPORT_EXPORTED", AuditService.MODULE_STATISTICS,
                "Reporte oficial", kind.name(), kind + " " + format + ", " + table.rows().size() + " filas, "
                        + String.join("; ", table.filters()));
        return new ExportResult(content, filename(kind, filters, format), ExportJobRunner.contentType(format), null);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> jobs() {
        return jobRepository.findTop20ByRequestedByOrderByCreatedAtDesc(currentUser.require().id()).stream()
                .map(JobResponse::from)
                .toList();
    }

    @Transactional
    public JobFile jobFile(String id) {
        AuthenticatedUser actor = currentUser.require();
        ExportJobEntity job = jobRepository.findById(id)
                .filter(found -> found.getRequestedBy().equals(actor.id()))
                .orElseThrow(() -> ApiException.of(ErrorCode.EXPORT_NOT_FOUND));
        if (job.getStatus() != ExportJobEntity.Status.DONE) {
            throw ApiException.of(ErrorCode.EXPORT_NOT_READY);
        }
        auditService.record(AuditActionType.SECURITY, "REPORT_EXPORT_DOWNLOADED", AuditService.MODULE_STATISTICS,
                "Reporte oficial", job.getId(), job.getKind() + " " + job.getFormat());
        return new JobFile(storage.load(job.getStoragePath()), job.getFilename(),
                ExportJobRunner.contentType(job.getFormat()));
    }

    /** Procesa los trabajos pendientes en el hilo actual (arranque del servidor y pruebas). */
    public int runPending() {
        List<ExportJobEntity> pending = jobRepository.findByStatus(ExportJobEntity.Status.PENDING);
        pending.forEach(job -> runner.run(job.getId()));
        return pending.size();
    }

    /** El trabajo se lanza después del commit: el hilo de fondo debe poder leerlo ya guardado. */
    private void submitAfterCommit(String jobId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(() -> runner.run(jobId));
                }
            });
        } else {
            executor.execute(() -> runner.run(jobId));
        }
    }

    static String filename(OfficialReportKind kind, ReportFilters filters, ExportJobEntity.Format format) {
        String base = switch (kind) {
            case LANDINGS -> "desembarcos";
            case QUOTA_COMPLIANCE -> "cumplimiento-cuotas";
            case PERMITS -> "permisos";
            case ILLEGAL_REPORTS -> "denuncias";
            case ZONE_EVENTS -> "ingresos-zonas";
        };
        String period = kind == OfficialReportKind.PERMITS ? "" : "_" + filters.from() + "_" + filters.to();
        return "sigepes_" + base + period + "." + format.name().toLowerCase();
    }
}
