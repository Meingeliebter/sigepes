-- =====================================================================
-- SIGEPES · MVP v1.1 · RF-10 / HU-10 · Denuncias de pesca ilegal
--   RN-20: la denuncia anónima no guarda ningún vínculo con la cuenta del
--          denunciante. reporter_id queda NULL y no existe columna de IP,
--          correo ni dispositivo (AC-10.5).
-- =====================================================================

CREATE TABLE illegal_reports (
    id               VARCHAR(40)   NOT NULL,
    tracking_code    VARCHAR(20)   NOT NULL,
    -- Identificador generado en el dispositivo: un reenvío desde la cola offline no duplica la denuncia.
    client_id        VARCHAR(64),
    report_type      VARCHAR(30)   NOT NULL,
    description      VARCHAR(2000) NOT NULL,
    occurred_on      DATE          NOT NULL,
    latitude         NUMERIC(9, 6),
    longitude        NUMERIC(9, 6),
    location_text    VARCHAR(200),
    anonymous        BOOLEAN       NOT NULL,
    reporter_id      VARCHAR(40),
    status           VARCHAR(20)   NOT NULL DEFAULT 'RECEIVED',
    resolution_note  VARCHAR(1000),
    demo             BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_illegal_reports PRIMARY KEY (id),
    CONSTRAINT uq_illegal_reports_code UNIQUE (tracking_code),
    CONSTRAINT uq_illegal_reports_client UNIQUE (client_id),
    CONSTRAINT fk_illegal_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT ck_illegal_reports_type CHECK (report_type IN
        ('CLOSURE_FISHING', 'UNDERSIZED_CATCH', 'PROHIBITED_GEAR', 'MPA_INTRUSION', 'OTHER')),
    CONSTRAINT ck_illegal_reports_status CHECK (status IN
        ('RECEIVED', 'INVESTIGATING', 'CLOSED_FOUNDED', 'CLOSED_UNFOUNDED')),
    CONSTRAINT ck_illegal_reports_anonymous CHECK (anonymous = FALSE OR reporter_id IS NULL),
    CONSTRAINT ck_illegal_reports_lat CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_illegal_reports_lon CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX ix_illegal_reports_status ON illegal_reports (status, created_at);
CREATE INDEX ix_illegal_reports_reporter ON illegal_reports (reporter_id);

-- Fotos ya saneadas: sin EXIF y sin el nombre original del archivo.
CREATE TABLE report_photos (
    id            VARCHAR(40)  NOT NULL,
    report_id     VARCHAR(40)  NOT NULL,
    storage_path  VARCHAR(300) NOT NULL,
    content_type  VARCHAR(40)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_report_photos PRIMARY KEY (id),
    CONSTRAINT fk_report_photos_report FOREIGN KEY (report_id) REFERENCES illegal_reports (id) ON DELETE CASCADE
);

CREATE INDEX ix_report_photos_report ON report_photos (report_id);

-- Historial del flujo de inspección (AC-10.4): quién movió la denuncia, a qué estado y con qué nota.
CREATE TABLE report_status_changes (
    id           VARCHAR(40)   NOT NULL,
    report_id    VARCHAR(40)   NOT NULL,
    from_status  VARCHAR(20)   NOT NULL,
    to_status    VARCHAR(20)   NOT NULL,
    note         VARCHAR(1000) NOT NULL,
    changed_by   VARCHAR(40)   NOT NULL,
    changed_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_report_status_changes PRIMARY KEY (id),
    CONSTRAINT fk_report_status_changes_report FOREIGN KEY (report_id) REFERENCES illegal_reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_status_changes_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX ix_report_status_changes_report ON report_status_changes (report_id, changed_at);
