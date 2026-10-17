package com.sigepes.backend.reports;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IllegalReportRepository extends JpaRepository<IllegalReportEntity, String> {

    Optional<IllegalReportEntity> findByTrackingCode(String trackingCode);

    Optional<IllegalReportEntity> findByClientId(String clientId);

    boolean existsByTrackingCode(String trackingCode);

    List<IllegalReportEntity> findAllByOrderByCreatedAtDesc();

    List<IllegalReportEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<IllegalReportEntity> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    /** RF-12 · Denuncias por estado radicadas en un periodo: filas {@code [status, cantidad]}. */
    @Query("SELECT r.status, COUNT(r) FROM IllegalReportEntity r WHERE r.createdAt BETWEEN :from AND :to GROUP BY r.status")
    List<Object[]> countByStatusBetween(@Param("from") Instant from, @Param("to") Instant to);
}
