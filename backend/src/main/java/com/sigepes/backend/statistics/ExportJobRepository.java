package com.sigepes.backend.statistics;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportJobRepository extends JpaRepository<ExportJobEntity, String> {

    List<ExportJobEntity> findTop20ByRequestedByOrderByCreatedAtDesc(String requestedBy);

    List<ExportJobEntity> findByStatus(ExportJobEntity.Status status);
}
