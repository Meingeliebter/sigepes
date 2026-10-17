-- =====================================================================
-- SIGEPES · MVP v1.1 · RF-11 / HU-11 · Alertas y notificaciones de cumplimiento
--   AC-11.5: cada alerta se persiste en la misma transacción que el hecho que
--            la origina (cuota, ingreso a zona, denuncia...) y antes de
--            enviarse; si el servicio se reinicia, el buzón la conserva.
--   RN-22:  las alertas críticas no se pueden archivar.
-- =====================================================================

CREATE TABLE alerts (
    id            VARCHAR(40)   NOT NULL,
    recipient_id  VARCHAR(40)   NOT NULL,
    alert_type    VARCHAR(20)   NOT NULL,
    severity      VARCHAR(10)   NOT NULL,
    title         VARCHAR(160)  NOT NULL,
    message       VARCHAR(600)  NOT NULL,
    -- Ruta de la app donde está el registro relacionado (AC-11.2).
    link          VARCHAR(200),
    -- Clave del hecho que originó la alerta: evita duplicados al reprocesar (p. ej. permit:per-1:15).
    source_key    VARCHAR(160)  NOT NULL,
    read_at       TIMESTAMP,
    archived_at   TIMESTAMP,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_alerts PRIMARY KEY (id),
    CONSTRAINT uq_alerts_source UNIQUE (recipient_id, source_key),
    CONSTRAINT fk_alerts_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_alerts_type CHECK (alert_type IN ('CLOSURE', 'QUOTA', 'PERMIT', 'ZONE', 'REPORT', 'SECURITY')),
    CONSTRAINT ck_alerts_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_alerts_archive CHECK (archived_at IS NULL OR severity <> 'CRITICAL')
);

CREATE INDEX ix_alerts_inbox ON alerts (recipient_id, archived_at, created_at);
CREATE INDEX ix_alerts_unread ON alerts (recipient_id, read_at);
