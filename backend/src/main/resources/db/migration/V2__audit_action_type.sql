-- =====================================================================
-- SIGEPES · RF-20 · Tipo de acción normalizado en la bitácora de auditoría
--   AC-20.2: cada entrada debe clasificarse como CREATE/UPDATE/DELETE
--            (o SECURITY, para eventos de autenticación).
-- =====================================================================

ALTER TABLE audit_logs ADD COLUMN action_type VARCHAR(20);

-- Backfill heurístico para filas ya existentes (no hay datos reales en producción todavía):
-- los eventos con estado antes/después son actualizaciones; los de sesión son de seguridad;
-- el resto se asume alta.
UPDATE audit_logs SET action_type = 'UPDATE'
    WHERE action_type IS NULL AND before_state IS NOT NULL AND after_state IS NOT NULL;
UPDATE audit_logs SET action_type = 'SECURITY'
    WHERE action_type IS NULL AND action IN ('LOGIN', 'LOGIN_FAILED', 'LOGIN_BLOCKED', 'LOGOUT');
UPDATE audit_logs SET action_type = 'CREATE' WHERE action_type IS NULL;

ALTER TABLE audit_logs ALTER COLUMN action_type SET NOT NULL;
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action_type
    CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'SECURITY'));

-- ix_audit_actor (actor_id) e ix_audit_occurred_at (occurred_at) ya existen desde V1.
CREATE INDEX ix_audit_logs_action_type ON audit_logs (action_type);
