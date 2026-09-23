package com.sigepes.backend.audit;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findAllByOrderByOccurredAtDesc(Limit limit);

    List<AuditLogEntity> findByModuleOrderByOccurredAtDesc(String module, Limit limit);

    long countByAction(String action);

    /** AC-20.4: combina libremente módulo, tipo de acción, actor y rango de fechas. */
    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE (:module IS NULL OR a.module = :module)
              AND (:actionType IS NULL OR a.actionType = :actionType)
              AND (:actorId IS NULL OR a.actorId = :actorId)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to IS NULL OR a.occurredAt <= :to)
            ORDER BY a.occurredAt DESC
            """)
    List<AuditLogEntity> search(@Param("module") String module, @Param("actionType") AuditActionType actionType,
            @Param("actorId") String actorId, @Param("from") Instant from, @Param("to") Instant to, Limit limit);

    /** AC-20.5: IPs con al menos {@code threshold} inicios de sesión fallidos desde {@code since}. */
    @Query("""
            SELECT a.ipAddress AS ipAddress, COUNT(a) AS failures FROM AuditLogEntity a
            WHERE a.action = 'LOGIN_FAILED' AND a.occurredAt >= :since AND a.ipAddress IS NOT NULL
            GROUP BY a.ipAddress
            HAVING COUNT(a) >= :threshold
            ORDER BY COUNT(a) DESC
            """)
    List<SuspiciousIp> findSuspiciousLoginIps(@Param("since") Instant since, @Param("threshold") long threshold);

    /** Proyección de la agregación anterior. */
    interface SuspiciousIp {
        String getIpAddress();

        long getFailures();
    }
}
