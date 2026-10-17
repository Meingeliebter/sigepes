package com.sigepes.backend.reports;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportPhotoRepository extends JpaRepository<ReportPhotoEntity, String> {

    List<ReportPhotoEntity> findByReportIdOrderByCreatedAtAsc(String reportId);

    Optional<ReportPhotoEntity> findByIdAndReportId(String id, String reportId);

    /** Filas {@code [reportId, cantidad]} para la bandeja, en una sola consulta. */
    @Query("select p.reportId, count(p) from ReportPhotoEntity p group by p.reportId")
    List<Object[]> countByReport();
}
