package com.sigepes.backend.geo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripPositionRepository extends JpaRepository<TripPositionEntity, String> {

    List<TripPositionEntity> findByTripIdOrderByRecordedAtAsc(String tripId);

    Optional<TripPositionEntity> findTopByTripIdOrderByRecordedAtDesc(String tripId);
}
