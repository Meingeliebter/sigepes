-- =====================================================================
-- SIGEPES · MVP v1.1 · RF-18 / HU-18 · Reportes y estadísticas oficiales
--   AC-18.5: las exportaciones de más de un año se generan en segundo plano;
--            este registro sigue su estado y el archivo resultante.
-- =====================================================================

CREATE TABLE export_jobs (
    id             VARCHAR(40)   NOT NULL,
    requested_by   VARCHAR(40)   NOT NULL,
    report_kind    VARCHAR(30)   NOT NULL,
    format         VARCHAR(5)    NOT NULL,
    period_from    DATE          NOT NULL,
    period_to      DATE          NOT NULL,
    region         VARCHAR(20),
    species_id     VARCHAR(40),
    status         VARCHAR(10)   NOT NULL,
    storage_path   VARCHAR(300),
    filename       VARCHAR(160),
    row_count      INTEGER,
    error          VARCHAR(300),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at    TIMESTAMP,
    CONSTRAINT pk_export_jobs PRIMARY KEY (id),
    CONSTRAINT fk_export_jobs_user FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT ck_export_jobs_format CHECK (format IN ('CSV', 'PDF')),
    CONSTRAINT ck_export_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED'))
);

CREATE INDEX ix_export_jobs_user ON export_jobs (requested_by, created_at);

-- AC-18.5: "tu reporte está listo" llega por el buzón de alertas (HU-11).
ALTER TABLE alerts DROP CONSTRAINT ck_alerts_type;
ALTER TABLE alerts ADD CONSTRAINT ck_alerts_type CHECK (alert_type IN
    ('CLOSURE', 'QUOTA', 'PERMIT', 'ZONE', 'REPORT', 'SECURITY', 'DOCUMENT', 'EXPORT'));
