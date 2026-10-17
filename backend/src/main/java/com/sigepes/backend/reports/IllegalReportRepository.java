package com.sigepes.backend.reports;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IllegalReportRepository extends JpaRepository<IllegalReportEntity, String> {

    Optional<IllegalReportEntity> findByTrackingCode(String trackingCode);

    Optional<IllegalReportEntity> findByClientId(String clientId);

    boolean existsByTrackingCode(String trackingCode);

    List<IllegalReportEntity> findAllByOrderByCreatedAtDesc();

    List<IllegalReportEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<IllegalReportEntity> findByReporterIdOrderByCreatedAtDesc(String reporterId);
}
