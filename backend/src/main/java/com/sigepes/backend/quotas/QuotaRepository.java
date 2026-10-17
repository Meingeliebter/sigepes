package com.sigepes.backend.quotas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface QuotaRepository extends JpaRepository<QuotaEntity, String> {

    List<QuotaEntity> findAllByOrderByPeriodStartDescCreatedAtDesc();

    /** Cuotas activas de una especie cuyo periodo contiene la fecha (usa ix_quotas_lookup). */
    @Query("""
            SELECT q FROM QuotaEntity q
            WHERE q.species.id = :speciesId
              AND q.status = com.sigepes.backend.quotas.QuotaStatus.ACTIVE
              AND q.periodStart <= :date
              AND q.periodEnd >= :date
            """)
    List<QuotaEntity> findActiveFor(@Param("speciesId") String speciesId, @Param("date") LocalDate date);

    /** Bloqueo pesimista de la fila para descontar sin perder kilos entre cierres concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QuotaEntity q WHERE q.id = :id")
    Optional<QuotaEntity> lockById(@Param("id") String id);
}
