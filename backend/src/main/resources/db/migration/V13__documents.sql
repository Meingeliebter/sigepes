-- =====================================================================
-- SIGEPES · MVP v1.1 · RF-15 / HU-15 · Gestión documental versionada
--   RN-30: una nueva carga crea una versión; las anteriores nunca se
--          sobrescriben ni se borran.
--   AC-15.6: los archivos viven en el disco persistente (UPLOADS_DIR), fuera
--            del contenedor; aquí solo se guardan sus metadatos y su hash.
-- =====================================================================

CREATE TABLE documents (
    id               VARCHAR(40)   NOT NULL,
    owner_id         VARCHAR(40)   NOT NULL,
    doc_type         VARCHAR(30)   NOT NULL,
    title            VARCHAR(160)  NOT NULL,
    entity_type      VARCHAR(20)   NOT NULL,
    entity_id        VARCHAR(40)   NOT NULL,
    current_version  INTEGER       NOT NULL,
    -- Vencimiento de la versión vigente (copiado para filtrar y alertar sin unir tablas).
    expires_on       DATE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documents PRIMARY KEY (id),
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_documents_type CHECK (doc_type IN
        ('ID_DOCUMENT', 'VESSEL_CERTIFICATE', 'PERMIT', 'RESOLUTION', 'OTHER')),
    CONSTRAINT ck_documents_entity CHECK (entity_type IN ('USER', 'VESSEL', 'PERMIT', 'CLOSURE'))
);

CREATE INDEX ix_documents_owner ON documents (owner_id, updated_at);
CREATE INDEX ix_documents_entity ON documents (entity_type, entity_id);
CREATE INDEX ix_documents_expiry ON documents (expires_on);

CREATE TABLE document_versions (
    id                VARCHAR(40)   NOT NULL,
    document_id       VARCHAR(40)   NOT NULL,
    version_no        INTEGER       NOT NULL,
    storage_path      VARCHAR(300)  NOT NULL,
    original_filename VARCHAR(160)  NOT NULL,
    content_type      VARCHAR(40)   NOT NULL,
    size_bytes        BIGINT        NOT NULL,
    sha256            VARCHAR(64)   NOT NULL,
    expires_on        DATE,
    note              VARCHAR(300),
    uploaded_by       VARCHAR(40)   NOT NULL,
    uploaded_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_versions PRIMARY KEY (id),
    CONSTRAINT uq_document_versions_no UNIQUE (document_id, version_no),
    CONSTRAINT fk_document_versions_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_document_versions_user FOREIGN KEY (uploaded_by) REFERENCES users (id)
);

-- AC-15.3: el vencimiento de documentos alimenta el buzón de alertas (HU-11).
ALTER TABLE alerts DROP CONSTRAINT ck_alerts_type;
ALTER TABLE alerts ADD CONSTRAINT ck_alerts_type CHECK (alert_type IN
    ('CLOSURE', 'QUOTA', 'PERMIT', 'ZONE', 'REPORT', 'SECURITY', 'DOCUMENT'));
