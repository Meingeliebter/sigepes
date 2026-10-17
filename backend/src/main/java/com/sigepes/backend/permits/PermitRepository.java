package com.sigepes.backend.permits;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermitRepository extends JpaRepository<PermitEntity, String> {

    Optional<PermitEntity> findByNumberIgnoreCase(String number);

    Optional<PermitEntity> findByVerificationCode(String verificationCode);

    boolean existsByNumberIgnoreCase(String number);

    List<PermitEntity> findAllByOrderByExpiresAtDesc();

    List<PermitEntity> findByOwnerIdOrderByExpiresAtDesc(String ownerId);

    List<PermitEntity> findByOwnerIdAndStatus(String ownerId, PermitStatus status);

    /** Permisos aprobados de un titular cuya vigencia cubre la fecha indicada (RN-16). */
    @Query("""
            SELECT p FROM PermitEntity p
            WHERE p.owner.id = :ownerId
              AND p.status = com.sigepes.backend.permits.PermitStatus.APPROVED
              AND p.issuedAt <= :date
              AND p.expiresAt >= :date
            ORDER BY p.expiresAt DESC
            """)
    List<PermitEntity> findValidFor(@Param("ownerId") String ownerId, @Param("date") LocalDate date);

    long countByStatus(PermitStatus status);
}
