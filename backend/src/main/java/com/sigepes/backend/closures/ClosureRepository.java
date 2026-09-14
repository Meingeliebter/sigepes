package com.sigepes.backend.closures;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClosureRepository extends JpaRepository<ClosureEntity, String> {

    List<ClosureEntity> findAllByOrderByStartDateDesc();

    List<ClosureEntity> findBySpeciesId(String speciesId);

    /** Vedas no levantadas cuyo periodo contiene la fecha indicada. */
    @Query("""
            SELECT c FROM ClosureEntity c
            WHERE c.status <> com.sigepes.backend.closures.ClosureStatus.FINISHED
              AND c.startDate <= :date
              AND c.endDate >= :date
            ORDER BY c.startDate DESC
            """)
    List<ClosureEntity> findInForceOn(@Param("date") LocalDate date);

    /** Vedas no levantadas que afectan a una especie en una fecha dada. */
    @Query("""
            SELECT c FROM ClosureEntity c
            WHERE c.species.id = :speciesId
              AND c.status <> com.sigepes.backend.closures.ClosureStatus.FINISHED
              AND c.startDate <= :date
              AND c.endDate >= :date
            """)
    List<ClosureEntity> findInForceForSpecies(@Param("speciesId") String speciesId, @Param("date") LocalDate date);
}
