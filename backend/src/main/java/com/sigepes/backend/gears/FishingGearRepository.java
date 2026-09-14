package com.sigepes.backend.gears;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FishingGearRepository extends JpaRepository<FishingGearEntity, String> {

    Optional<FishingGearEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<FishingGearEntity> findAllByOrderByNameAsc();
}
