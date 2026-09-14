package com.sigepes.backend.gears;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.gears.GearDtos.CreateGearRequest;

/** RF-02 · Catálogo de artes de pesca. */
@Service
public class GearService {

    private final FishingGearRepository repository;
    private final AuditService auditService;

    public GearService(FishingGearRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<FishingGearEntity> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public FishingGearEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.GEAR_NOT_FOUND));
    }

    /**
     * Resuelve un arte por identificador o, si no existe, por nombre.
     *
     * <p>La interfaz trabaja con nombres de arte ({@code Vessel.gears: string[]}), así que se
     * aceptan ambas formas.
     */
    @Transactional(readOnly = true)
    public FishingGearEntity resolve(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            throw ApiException.of(ErrorCode.GEAR_NOT_FOUND);
        }
        String value = idOrName.trim();
        return repository.findById(value)
                .or(() -> repository.findByNameIgnoreCase(value))
                .orElseThrow(() -> ApiException.of(ErrorCode.GEAR_NOT_FOUND,
                        "El arte de pesca «" + value + "» no está en el catálogo."));
    }

    @Transactional
    public FishingGearEntity create(CreateGearRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw ApiException.of(ErrorCode.GEAR_ALREADY_REGISTERED);
        }
        boolean destructive = Boolean.TRUE.equals(request.destructive());
        GearStatus status = request.status() == null || request.status().isBlank()
                ? (destructive ? GearStatus.BLOCKED : GearStatus.ACTIVE)
                : GearStatus.valueOf(request.status());

        FishingGearEntity gear = repository.save(new FishingGearEntity(
                Ids.generate("art"), name, request.description(), destructive, status));

        auditService.record("GEAR_CREATED", AuditService.MODULE_GEARS, "Arte de pesca", gear.getId(),
                "Alta de arte «" + gear.getName() + "»" + (destructive ? " marcada como destructiva" : ""));
        return gear;
    }
}
