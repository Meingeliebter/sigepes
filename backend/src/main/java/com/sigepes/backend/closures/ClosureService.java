package com.sigepes.backend.closures;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.closures.ClosureDtos.ClosureResponse;
import com.sigepes.backend.closures.ClosureDtos.CreateClosureRequest;
import com.sigepes.backend.closures.ClosureDtos.UpdateClosureRequest;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.species.SpeciesService;
import com.sigepes.backend.users.UserService;

/** RF-05 · Gestión de vedas y temporadas. */
@Service
public class ClosureService {

    private final ClosureRepository repository;
    private final SpeciesService speciesService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public ClosureService(ClosureRepository repository, SpeciesService speciesService, UserService userService,
            CurrentUser currentUser, AuditService auditService) {
        this.repository = repository;
        this.speciesService = speciesService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    /** Todas las vedas, para la pantalla de vedas. */
    @Transactional(readOnly = true)
    public List<ClosureResponse> findAll() {
        LocalDate today = AppClock.today();
        return repository.findAllByOrderByStartDateDesc().stream()
                .map(closure -> ClosureResponse.from(closure, today))
                .toList();
    }

    /** Solo las vedas vigentes hoy, para la validación previa del formulario de faena. */
    @Transactional(readOnly = true)
    public List<ClosureResponse> findInForce() {
        LocalDate today = AppClock.today();
        return repository.findInForceOn(today).stream()
                .map(closure -> ClosureResponse.from(closure, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClosureEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.CLOSURE_NOT_FOUND));
    }

    @Transactional
    public ClosureEntity create(CreateClosureRequest request) {
        if (request.to().isBefore(request.from())) {
            throw ApiException.of(ErrorCode.INVALID_CLOSURE_PERIOD,
                    "La fecha de finalización no puede ser anterior a la de inicio.");
        }
        SpeciesEntity species = speciesService.byId(request.speciesId());

        ClosureEntity closure = new ClosureEntity(Ids.generate("ved"), species, request.zone().trim(),
                request.from(), request.to(), ClosureStatus.SCHEDULED, request.resolutionNumber().trim(),
                ClosureType.valueOf(request.closureType()));
        closure.setStatus(closure.effectiveStatus(AppClock.today()));
        closure.setReason(request.reason());
        closure.setNotes(request.notes());
        currentUser.find().ifPresent(user -> closure.setCreatedBy(userService.byId(user.id())));

        ClosureEntity saved = repository.save(closure);
        auditService.record(AuditActionType.CREATE, "CLOSURE_CREATED", AuditService.MODULE_CLOSURES, "Veda",
                saved.getId(), "Veda de «" + species.getCommonName() + "» en " + saved.getZone() + " del "
                        + saved.getStartDate() + " al " + saved.getEndDate());
        return saved;
    }

    @Transactional
    public ClosureEntity update(String id, UpdateClosureRequest request) {
        ClosureEntity closure = byId(id);
        String before = describe(closure);

        LocalDate start = request.from() != null ? request.from() : closure.getStartDate();
        LocalDate end = request.to() != null ? request.to() : closure.getEndDate();
        if (end.isBefore(start)) {
            throw ApiException.of(ErrorCode.INVALID_CLOSURE_PERIOD,
                    "La fecha de finalización no puede ser anterior a la de inicio.");
        }
        closure.setStartDate(start);
        closure.setEndDate(end);
        if (request.notes() != null) {
            closure.setNotes(request.notes());
        }
        if (request.resolutionNumber() != null && !request.resolutionNumber().isBlank()) {
            closure.setResolutionNumber(request.resolutionNumber().trim());
        }
        if (request.closureType() != null && !request.closureType().isBlank()) {
            closure.setClosureType(ClosureType.valueOf(request.closureType()));
        }
        closure.setStatus(request.status() != null && !request.status().isBlank()
                ? ClosureStatus.valueOf(request.status())
                : closure.effectiveStatus(AppClock.today()));

        ClosureEntity saved = repository.save(closure);
        auditService.recordChange("CLOSURE_UPDATED", AuditService.MODULE_CLOSURES, "Veda", saved.getId(),
                "Actualización de la veda de «" + saved.getSpecies().getCommonName() + "»", before, describe(saved));
        return saved;
    }

    private static String describe(ClosureEntity closure) {
        return AuditService.toJson(Map.of(
                "zone", closure.getZone(),
                "from", closure.getStartDate().toString(),
                "to", closure.getEndDate().toString(),
                "status", closure.getStatus().name(),
                "closureType", closure.getClosureType().name(),
                "resolutionNumber", closure.getResolutionNumber()));
    }
}
