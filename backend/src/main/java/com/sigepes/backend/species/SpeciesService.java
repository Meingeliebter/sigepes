package com.sigepes.backend.species;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.species.SpeciesDtos.CreateSpeciesRequest;
import com.sigepes.backend.species.SpeciesDtos.SpeciesResponse;
import com.sigepes.backend.species.SpeciesDtos.UpdateSpeciesRequest;

/** RF-06 · Catálogo de especies y tallas mínimas. */
@Service
public class SpeciesService {

    private final SpeciesRepository repository;
    private final ClosureRepository closureRepository;
    private final AuditService auditService;

    public SpeciesService(SpeciesRepository repository, ClosureRepository closureRepository,
            AuditService auditService) {
        this.repository = repository;
        this.closureRepository = closureRepository;
        this.auditService = auditService;
    }

    /** Lista el catálogo marcando qué especies tienen veda vigente hoy. */
    @Transactional(readOnly = true)
    public List<SpeciesResponse> listWithClosureState() {
        LocalDate today = LocalDate.now();
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
        boolean underClosure = !closureRepository.findInForceForSpecies(id, LocalDate.now()).isEmpty();
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
                        : SpeciesStatus.valueOf(request.status())));

        auditService.record("SPECIES_CREATED", AuditService.MODULE_SPECIES, "Especie", species.getId(),
                "Alta de «" + species.getCommonName() + "» con talla mínima " + species.getMinSizeCm() + " cm");
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
        if (request.status() != null && !request.status().isBlank()) {
            species.setStatus(SpeciesStatus.valueOf(request.status()));
        }

        SpeciesEntity saved = repository.save(species);
        auditService.recordChange("SPECIES_UPDATED", AuditService.MODULE_SPECIES, "Especie", saved.getId(),
                "Actualización de «" + saved.getCommonName() + "»", before, describe(saved));
        return saved;
    }

    private static String describe(SpeciesEntity species) {
        return "commonName=" + species.getCommonName() + "; minSizeCm=" + species.getMinSizeCm()
                + "; status=" + species.getStatus();
    }
}
