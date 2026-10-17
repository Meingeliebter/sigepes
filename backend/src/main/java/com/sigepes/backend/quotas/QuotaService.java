package com.sigepes.backend.quotas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.quotas.QuotaDtos.CreateQuotaRequest;
import com.sigepes.backend.quotas.QuotaDtos.QuotaResponse;
import com.sigepes.backend.quotas.QuotaDtos.UpdateQuotaRequest;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.species.SpeciesService;
import com.sigepes.backend.users.UserService;
import com.sigepes.backend.vessels.VesselService;

/**
 * RF-07 · Control de cuotas de pesca por especie (RN-14).
 *
 * <p>Dos momentos de control:
 * <ol>
 *   <li>al registrar la captura ({@link #requireCapacity}), para avisar antes de que el pescador
 *       termine la faena;</li>
 *   <li>al cerrar la faena ({@link #consume}), donde el descuento es definitivo y se hace con la
 *       fila de la cuota bloqueada, de modo que dos cierres simultáneos no se repartan el mismo
 *       saldo.</li>
 * </ol>
 */
@Service
public class QuotaService {

    /** AC-07.4: umbrales de consumo que generan alerta, de menor a mayor. */
    static final int[] THRESHOLDS = { 80, 95, 100 };

    private final QuotaRepository repository;
    private final SpeciesService speciesService;
    private final VesselService vesselService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public QuotaService(QuotaRepository repository, SpeciesService speciesService, VesselService vesselService,
            UserService userService, CurrentUser currentUser, AuditService auditService,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.speciesService = speciesService;
        this.vesselService = vesselService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<QuotaResponse> findAll() {
        return repository.findAllByOrderByPeriodStartDescCreatedAtDesc().stream().map(QuotaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public QuotaEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.QUOTA_NOT_FOUND));
    }

    /** AC-07.5: saldo de la cuota que aplica a una especie, zona, fecha y embarcación. */
    @Transactional(readOnly = true)
    public Optional<QuotaResponse> balance(String speciesId, String zone, LocalDate date, String vesselId) {
        return applicable(speciesId, zone, date, vesselId).map(QuotaResponse::from);
    }

    @Transactional
    public QuotaEntity create(CreateQuotaRequest request) {
        if (request.to().isBefore(request.from())) {
            throw ApiException.of(ErrorCode.INVALID_QUOTA_PERIOD,
                    "La fecha de finalización no puede ser anterior a la de inicio.");
        }
        SpeciesEntity species = speciesService.byId(request.speciesId());
        QuotaEntity quota = new QuotaEntity(Ids.generate("cuo"), species, request.zone().trim(), request.from(),
                request.to(), request.totalKg());
        if (request.vesselId() != null && !request.vesselId().isBlank()) {
            quota.setVessel(vesselService.byId(request.vesselId()));
        }
        quota.setResolutionNumber(blankToNull(request.resolutionNumber()));
        quota.setNotes(blankToNull(request.notes()));
        currentUser.find().ifPresent(user -> quota.setCreatedBy(userService.byId(user.id())));

        QuotaEntity saved = repository.save(quota);
        auditService.record(AuditActionType.CREATE, "QUOTA_CREATED", AuditService.MODULE_QUOTAS, "Cuota",
                saved.getId(), "Cuota de «" + species.getCommonName() + "» en " + saved.getZone() + ": "
                        + saved.getTotalKg() + " kg del " + saved.getPeriodStart() + " al " + saved.getPeriodEnd());
        return saved;
    }

    @Transactional
    public QuotaEntity update(String id, UpdateQuotaRequest request) {
        QuotaEntity quota = byId(id);
        String before = describe(quota);
        if (request.totalKg() != null) {
            if (request.totalKg().compareTo(quota.getConsumedKg()) < 0) {
                throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                        "La cuota no puede ser menor que lo ya consumido (" + quota.getConsumedKg() + " kg).");
            }
            quota.setTotalKg(request.totalKg());
        }
        if (request.to() != null) {
            if (request.to().isBefore(quota.getPeriodStart())) {
                throw ApiException.of(ErrorCode.INVALID_QUOTA_PERIOD,
                        "La fecha de finalización no puede ser anterior a la de inicio.");
            }
            quota.setPeriodEnd(request.to());
        }
        if (request.status() != null && !request.status().isBlank()) {
            quota.setStatus(QuotaStatus.valueOf(request.status()));
        }
        if (request.notes() != null) {
            quota.setNotes(blankToNull(request.notes()));
        }
        QuotaEntity saved = repository.save(quota);
        auditService.recordChange("QUOTA_UPDATED", AuditService.MODULE_QUOTAS, "Cuota", saved.getId(),
                "Actualización de la cuota de «" + saved.getSpecies().getCommonName() + "»", before, describe(saved));
        return saved;
    }

    /**
     * AC-07.3 · Validación previa al registrar la captura: la suma de lo ya consumido, lo pendiente
     * en la misma faena y la nueva captura no puede exceder la cuota.
     */
    @Transactional(readOnly = true)
    public void requireCapacity(SpeciesEntity species, String zone, LocalDate date, String vesselId,
            BigDecimal pendingInTripKg, BigDecimal weightKg) {
        applicable(species.getId(), zone, date, vesselId).ifPresent(quota -> {
            BigDecimal requested = pendingInTripKg.add(weightKg);
            if (!quota.canAbsorb(requested)) {
                throw exhausted(quota);
            }
        });
    }

    /**
     * AC-07.2 · Descuento definitivo de una captura al cerrar la faena, bajo bloqueo pesimista.
     *
     * @return el identificador de la cuota descontada, o {@code null} si la especie no tiene cuota
     */
    @Transactional
    public String consume(String speciesId, String zone, LocalDate date, String vesselId, String ownerId,
            BigDecimal weightKg) {
        Optional<QuotaEntity> candidate = applicable(speciesId, zone, date, vesselId);
        if (candidate.isEmpty()) {
            return null;
        }
        QuotaEntity quota = repository.lockById(candidate.get().getId()).orElseThrow();
        if (!quota.canAbsorb(weightKg)) {
            throw exhausted(quota);
        }
        quota.consume(weightKg);
        int crossed = highestCrossed(quota);
        if (crossed > quota.getLastThreshold()) {
            quota.setLastThreshold(crossed);
            events.publishEvent(new QuotaThresholdReached(quota.getId(), quota.getSpecies().getCommonName(),
                    quota.getZone(), ownerId, crossed, quota.consumedPercent().toPlainString(),
                    quota.remainingKg().toPlainString()));
        }
        repository.save(quota);
        auditService.record(AuditActionType.UPDATE, "QUOTA_CONSUMED", AuditService.MODULE_QUOTAS, "Cuota",
                quota.getId(), "Descuento de " + weightKg + " kg de «" + quota.getSpecies().getCommonName()
                        + "»: saldo " + quota.remainingKg() + " kg (" + quota.consumedPercent() + " % consumido)");
        return quota.getId();
    }

    private Optional<QuotaEntity> applicable(String speciesId, String zone, LocalDate date, String vesselId) {
        return repository.findActiveFor(speciesId, date).stream()
                .filter(quota -> quota.appliesTo(speciesId, zone, date, vesselId))
                .max(Comparator.comparingInt(QuotaEntity::specificity));
    }

    private static int highestCrossed(QuotaEntity quota) {
        int crossed = 0;
        for (int threshold : THRESHOLDS) {
            if (quota.consumedPercent().compareTo(BigDecimal.valueOf(threshold)) >= 0) {
                crossed = threshold;
            }
        }
        return crossed;
    }

    private static ApiException exhausted(QuotaEntity quota) {
        return ApiException.of(ErrorCode.QUOTA_EXHAUSTED,
                "Cuota agotada para " + quota.getSpecies().getCommonName() + " en " + quota.getZone()
                        + ": quedan " + quota.remainingKg() + " kg de " + quota.getTotalKg() + " kg.");
    }

    private static String describe(QuotaEntity quota) {
        return AuditService.toJson(Map.of(
                "totalKg", quota.getTotalKg().toPlainString(),
                "consumedKg", quota.getConsumedKg().toPlainString(),
                "to", quota.getPeriodEnd().toString(),
                "status", quota.getStatus().name()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
