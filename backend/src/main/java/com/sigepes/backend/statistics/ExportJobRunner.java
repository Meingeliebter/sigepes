package com.sigepes.backend.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sigepes.backend.alerts.AlertService;
import com.sigepes.backend.alerts.AlertSeverity;
import com.sigepes.backend.alerts.AlertType;
import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.FileStorageService;
import com.sigepes.backend.common.FileStorageService.StoredFile;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserService;

/**
 * AC-18.5 · Genera una exportación larga fuera de la petición HTTP y avisa al usuario por el buzón
 * de alertas (HU-11) cuando termina. Cada paso va en su propia transacción: si la generación
 * falla, el trabajo queda marcado como fallido en lugar de perderse.
 */
@Component
class ExportJobRunner {

    private static final Logger log = LoggerFactory.getLogger(ExportJobRunner.class);

    private final ExportJobRepository jobRepository;
    private final OfficialReportBuilder builder;
    private final ReportRenderer renderer;
    private final FileStorageService storage;
    private final UserService userService;
    private final AlertService alertService;
    private final AuditService auditService;
    private final TransactionTemplate tx;

    ExportJobRunner(ExportJobRepository jobRepository, OfficialReportBuilder builder, ReportRenderer renderer,
            FileStorageService storage, UserService userService, AlertService alertService, AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.builder = builder;
        this.renderer = renderer;
        this.storage = storage;
        this.userService = userService;
        this.alertService = alertService;
        this.auditService = auditService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    void run(String jobId) {
        try {
            tx.executeWithoutResult(status -> jobRepository.findById(jobId).ifPresent(job -> {
                job.start();
                jobRepository.save(job);
            }));
            tx.executeWithoutResult(status -> generate(jobId));
        } catch (RuntimeException e) {
            log.warn("La exportación {} falló: {}", jobId, e.getMessage());
            tx.executeWithoutResult(status -> jobRepository.findById(jobId).ifPresent(job -> {
                job.fail(e.getMessage());
                jobRepository.save(job);
                alertService.notify(job.getRequestedBy(), AlertType.EXPORT, AlertSeverity.WARNING,
                        "No se pudo generar tu reporte", "La exportación de «" + job.getKind().title()
                                + "» falló. Inténtalo de nuevo o acota el periodo.",
                        "/app/official-reports", "export:" + job.getId());
            }));
        }
    }

    private void generate(String jobId) {
        ExportJobEntity job = jobRepository.findById(jobId).orElseThrow();
        UserEntity user = userService.byId(job.getRequestedBy());
        StatTable table = builder.build(job.getKind(), job.filters(), user.getFullName());
        byte[] content = job.getFormat() == ExportJobEntity.Format.PDF ? renderer.pdf(table) : renderer.csv(table);
        String filename = OfficialReportService.filename(job.getKind(), job.filters(), job.getFormat());
        StoredFile stored = storage.storeBytes(content, contentType(job.getFormat()), "exportes", job.getId());
        job.finish(stored.storagePath(), filename, table.rows().size());
        jobRepository.save(job);
        alertService.notify(user.getId(), AlertType.EXPORT, AlertSeverity.INFO, "Tu reporte está listo",
                "«" + job.getKind().title() + "» (" + job.getFormat() + ", " + table.rows().size()
                        + " filas) ya se puede descargar.",
                "/app/official-reports", "export:" + job.getId());
        auditService.recordAs(user.getId(), user.getFullName(), AuditActionType.SECURITY, "REPORT_EXPORTED",
                AuditService.MODULE_STATISTICS, "Reporte oficial", job.getId(),
                job.getKind() + " " + job.getFormat() + " en segundo plano, " + String.join("; ", table.filters()),
                null, null);
    }

    static String contentType(ExportJobEntity.Format format) {
        return format == ExportJobEntity.Format.PDF ? "application/pdf" : "text/csv";
    }
}
