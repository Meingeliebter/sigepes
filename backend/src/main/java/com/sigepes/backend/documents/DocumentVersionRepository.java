package com.sigepes.backend.documents;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, String> {

    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNoDesc(String documentId);

    Optional<DocumentVersionEntity> findByDocumentIdAndVersionNo(String documentId, int versionNo);
}
