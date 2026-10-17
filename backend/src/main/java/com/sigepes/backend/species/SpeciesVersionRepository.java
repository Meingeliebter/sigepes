package com.sigepes.backend.species;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesVersionRepository extends JpaRepository<SpeciesVersionEntity, String> {

    List<SpeciesVersionEntity> findBySpeciesIdOrderByVersionDesc(String speciesId);

    Optional<SpeciesVersionEntity> findFirstBySpeciesIdOrderByVersionDesc(String speciesId);
}
