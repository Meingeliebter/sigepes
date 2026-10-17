package com.sigepes.backend.alerts;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<AlertEntity, String> {

    List<AlertEntity> findByRecipientIdAndArchivedAtIsNullOrderByCreatedAtDesc(String recipientId, Limit limit);

    long countByRecipientIdAndReadAtIsNullAndArchivedAtIsNull(String recipientId);

    boolean existsByRecipientIdAndSourceKey(String recipientId, String sourceKey);

    Optional<AlertEntity> findByIdAndRecipientId(String id, String recipientId);

    @Modifying
    @Query("update AlertEntity a set a.readAt = :now where a.recipientId = :recipientId and a.readAt is null")
    int markAllRead(@Param("recipientId") String recipientId, @Param("now") Instant now);
}
