package com.sigepes.backend.traceability;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChainEventRepository extends JpaRepository<ChainEventEntity, String> {

    List<ChainEventEntity> findByLotIdOrderBySequenceNoAsc(String lotId);
}
