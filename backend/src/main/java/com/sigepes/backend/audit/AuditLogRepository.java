package com.sigepes.backend.audit;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findAllByOrderByOccurredAtDesc(Limit limit);

    List<AuditLogEntity> findByModuleOrderByOccurredAtDesc(String module, Limit limit);

    long countByAction(String action);
}
