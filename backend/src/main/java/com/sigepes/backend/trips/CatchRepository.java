package com.sigepes.backend.trips;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatchRepository extends JpaRepository<CatchEntity, String> {

    long countByTripId(String tripId);

    /**
     * RF-12 · Desembarcos validados agregados por especie y zona: solo faenas cerradas (RN-24).
     * Filas {@code [speciesId, commonName, zone, kg, faenas]}.
     */
    @Query("""
            SELECT c.species.id, c.species.commonName, t.zone, SUM(c.weightKg), COUNT(DISTINCT t.id)
            FROM CatchEntity c JOIN c.trip t
            WHERE t.status = com.sigepes.backend.trips.TripStatus.CLOSED AND t.tripDate BETWEEN :from AND :to
            GROUP BY c.species.id, c.species.commonName, t.zone
            """)
    List<Object[]> landings(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Igual que {@link #landings}, limitado a las faenas de un titular (AC-12.6). */
    @Query("""
            SELECT c.species.id, c.species.commonName, t.zone, SUM(c.weightKg), COUNT(DISTINCT t.id)
            FROM CatchEntity c JOIN c.trip t
            WHERE t.status = com.sigepes.backend.trips.TripStatus.CLOSED AND t.tripDate BETWEEN :from AND :to
              AND t.owner.id = :ownerId
            GROUP BY c.species.id, c.species.commonName, t.zone
            """)
    List<Object[]> landingsOf(@Param("ownerId") String ownerId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
