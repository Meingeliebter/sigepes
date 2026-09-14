package com.sigepes.backend.species;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepository extends JpaRepository<SpeciesEntity, String> {

    Optional<SpeciesEntity> findByCommonNameIgnoreCase(String commonName);

    boolean existsByCommonNameIgnoreCase(String commonName);

    List<SpeciesEntity> findAllByOrderByCommonNameAsc();
}
