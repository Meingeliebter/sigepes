package com.sigepes.backend.trips;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FishingTripRepository extends JpaRepository<FishingTripEntity, String> {

    List<FishingTripEntity> findAllByOrderByTripDateDescCreatedAtDesc();

    List<FishingTripEntity> findByOwnerIdOrderByTripDateDescCreatedAtDesc(String ownerId);

    long countByOwnerIdAndTripDateGreaterThanEqual(String ownerId, LocalDate from);

    long countByTripDateGreaterThanEqual(LocalDate from);

    /** RF-12 · Faenas por estado en un periodo (las abiertas no entran en los indicadores, RN-24). */
    List<FishingTripEntity> findByTripDateBetween(LocalDate from, LocalDate to);
}
