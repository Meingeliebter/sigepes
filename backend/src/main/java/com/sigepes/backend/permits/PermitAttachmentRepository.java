package com.sigepes.backend.permits;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermitAttachmentRepository extends JpaRepository<PermitAttachmentEntity, String> {

    List<PermitAttachmentEntity> findByPermitId(String permitId);

    Optional<PermitAttachmentEntity> findByPermitIdAndKind(String permitId, AttachmentKind kind);
}
