package com.sigepes.backend.traceability;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotRepository extends JpaRepository<LotEntity, String> {

    Optional<LotEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<LotEntity> findAllByOrderByCreatedAtDesc();

    List<LotEntity> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    /** Kilos de una captura ya convertidos en lotes: no se puede vender dos veces el mismo pescado. */
    @Query("SELECT COALESCE(SUM(l.weightKg), 0) FROM LotEntity l WHERE l.catchEntry.id = :catchId")
    BigDecimal lottedWeight(@Param("catchId") String catchId);
}
