package com.sigepes.backend.geo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneEventRepository extends JpaRepository<ZoneEventEntity, String> {

    List<ZoneEventEntity> findTop200ByOrderByOccurredAtDesc();

    List<ZoneEventEntity> findByTripIdOrderByOccurredAtAsc(String tripId);

    List<ZoneEventEntity> findByOccurredAtBetween(Instant from, Instant to);

    long countByStatus(String status);
}
