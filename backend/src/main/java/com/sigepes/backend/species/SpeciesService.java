package com.sigepes.backend.species;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.AppClock;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.security.AuthenticatedUser;
import com.sigepes.backend.security.CurrentUser;
import com.sigepes.backend.species.SpeciesDtos.CreateSpeciesRequest;
import com.sigepes.backend.species.SpeciesDtos.SpeciesResponse;
import com.sigepes.backend.species.SpeciesDtos.SpeciesVersionResponse;
import com.sigepes.backend.species.SpeciesDtos.UpdateSpeciesRequest;

/** RF-06 · Catálogo de especies y tallas mínimas. */
@Service
public class SpeciesService {

    private final SpeciesRepository repository;
    private final SpeciesVersionRepository versionRepository;
    private final ClosureRepository closureRepository;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public SpeciesService(SpeciesRepository repository, SpeciesVersionRepository versionRepository,
            ClosureRepository closureRepository, AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.closureRepository = closureRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    /** Lista el catálogo marcando qué especies tienen veda vigente hoy. */
    @Transactional(readOnly = true)
    public List<SpeciesResponse> listWithClosureState() {
        LocalDate today = AppClock.today();
        Set<String> closedToday = closureRepository.findInForceOn(today).stream()
                .map(closure -> closure.getSpecies().getId())
                .collect(Collectors.toSet());
        return repository.findAllByOrderByCommonNameAsc().stream()
                .map(species -> SpeciesResponse.from(species, closedToday.contains(species.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SpeciesEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.SPECIES_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public SpeciesResponse detail(String id) {
        SpeciesEntity species = byId(id);
        boolean underClosure = !closureRepository.findInForceForSpecies(id, AppClock.today()).isEmpty();
        return SpeciesResponse.from(species, underClosure);
    }

    /** Vedas vigentes hoy que afectan a una especie. */
    @Transactional(readOnly = true)
    public List<ClosureEntity> closuresInForce(String speciesId, LocalDate date) {
        return closureRepository.findInForceForSpecies(speciesId, date);
    }

    @Transactional
    public SpeciesEntity create(CreateSpeciesRequest request) {
        String commonName = request.commonName().trim();
        if (repository.existsByCommonNameIgnoreCase(commonName)) {
            throw ApiException.of(ErrorCode.SPECIES_ALREADY_REGISTERED);
        }
        SpeciesEntity species = repository.save(new SpeciesEntity(
                Ids.generate("esp"),
                commonName,
                request.scientificName().trim(),
                request.minSizeCm(),
                request.status() == null || request.status().isBlank()
                        ? SpeciesStatus.ACTIVE
                        : SpeciesStatus.valueOf(request.status()),
                MeasurementMethod.valueOf(request.measurementMethod()),
                SpeciesRegion.valueOf(request.region()),
                request.imageUrl().trim()));

        auditService.record(AuditActionType.CREATE, "SPECIES_CREATED", AuditService.MODULE_SPECIES, "Especie",
                species.getId(),
                "Alta de «" + species.getCommonName() + "» con talla mínima " + species.getMinSizeCm() + " cm");
        recordVersion(species);
        return species;
    }

    @Transactional
    public SpeciesEntity update(String id, UpdateSpeciesRequest request) {
        SpeciesEntity species = byId(id);
        String before = describe(species);

        if (request.commonName() != null && !request.commonName().isBlank()) {
            species.setCommonName(request.commonName().trim());
        }
        if (request.scientificName() != null && !request.scientificName().isBlank()) {
            species.setScientificName(request.scientificName().trim());
        }
        if (request.minSizeCm() != null) {
            species.setMinSizeCm(request.minSizeCm());
        }
        if (request.measurementMethod() != null && !request.measurementMethod().isBlank()) {
            species.setMeasurementMethod(MeasurementMethod.valueOf(request.measurementMethod()));
        }
        if (request.region() != null && !request.region().isBlank()) {
            species.setRegion(SpeciesRegion.valueOf(request.region()));
        }
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            species.setImageUrl(request.imageUrl().trim());
        }
        if (request.status() != null && !request.status().isBlank()) {
            species.setStatus(SpeciesStatus.valueOf(request.status()));
        }

        SpeciesEntity saved = repository.save(species);
        auditService.recordChange("SPECIES_UPDATED", AuditService.MODULE_SPECIES, "Especie", saved.getId(),
                "Actualización de «" + saved.getCommonName() + "»", before, describe(saved));
        recordVersion(saved);
        return saved;
    }

    /** AC-06.4: historial de versiones de una especie, más reciente primero. */
    @Transactional(readOnly = true)
    public List<SpeciesVersionResponse> versions(String speciesId) {
        byId(speciesId); // valida que exista
        return versionRepository.findBySpeciesIdOrderByVersionDesc(speciesId).stream()
                .map(SpeciesVersionResponse::from)
                .toList();
    }

    /** AC-06.4: agrega un snapshot append-only cada vez que la especie se crea o edita. */
    private void recordVersion(SpeciesEntity species) {
        int nextVersion = versionRepository.findFirstBySpeciesIdOrderByVersionDesc(species.getId())
                .map(v -> v.getVersion() + 1)
                .orElse(1);
        AuthenticatedUser actor = currentUser.find().orElse(null);
        versionRepository.save(new SpeciesVersionEntity(
                Ids.generate("esv"),
                species.getId(),
                nextVersion,
                species.getCommonName(),
                species.getScientificName(),
                species.getMinSizeCm(),
                species.getMeasurementMethod(),
                species.getRegion(),
                species.getImageUrl(),
                species.getStatus(),
                actor == null ? null : actor.id(),
                actor == null ? "sistema" : actor.fullName()));
    }

    private static String describe(SpeciesEntity species) {
        return AuditService.toJson(Map.of(
                "commonName", species.getCommonName(),
                "minSizeCm", species.getMinSizeCm(),
                "measurementMethod", species.getMeasurementMethod().name(),
                "region", species.getRegion().name(),
                "status", species.getStatus().name()));
    }
}
