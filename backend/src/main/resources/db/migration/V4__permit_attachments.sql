-- =====================================================================
-- SIGEPES · RF-08 · Adjuntos obligatorios de la solicitud de permiso
--   AC-08.5: copia de cédula y certificado de propiedad de la embarcación.
-- =====================================================================

CREATE TABLE permit_attachments (
    id                 VARCHAR(40)  NOT NULL,
    permit_id          VARCHAR(40)  NOT NULL,
    kind               VARCHAR(20)  NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    size_bytes         BIGINT       NOT NULL,
    storage_path       VARCHAR(255) NOT NULL,
    uploaded_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_permit_attachments PRIMARY KEY (id),
    CONSTRAINT fk_permit_attachments_permit FOREIGN KEY (permit_id) REFERENCES permits (id) ON DELETE CASCADE,
    CONSTRAINT ck_permit_attachments_kind CHECK (kind IN ('ID_COPY', 'VESSEL_OWNERSHIP'))
);

CREATE INDEX ix_permit_attachments_permit ON permit_attachments (permit_id);
