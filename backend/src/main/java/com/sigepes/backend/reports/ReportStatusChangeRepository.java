package com.sigepes.backend.reports;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportStatusChangeRepository extends JpaRepository<ReportStatusChangeEntity, String> {

    List<ReportStatusChangeEntity> findByReportIdOrderByChangedAtAsc(String reportId);
}
