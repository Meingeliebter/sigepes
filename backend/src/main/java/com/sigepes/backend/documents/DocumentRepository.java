package com.sigepes.backend.documents;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

    List<DocumentEntity> findAllByOrderByUpdatedAtDesc();

    List<DocumentEntity> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    /** AC-15.3: documentos con vencimiento dentro de la ventana de aviso. */
    List<DocumentEntity> findByExpiresOnBetween(LocalDate from, LocalDate to);
}
