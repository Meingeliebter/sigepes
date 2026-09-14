package com.sigepes.backend.trips;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CatchRepository extends JpaRepository<CatchEntity, String> {

    long countByTripId(String tripId);
}
