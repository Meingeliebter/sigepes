-- =====================================================================
-- SIGEPES · MVP v1.1 · RF-07 / HU-07 · Control de cuotas de pesca por especie
--   RN-14: no se registra captura que exceda la cuota disponible.
--   AC-07.5: el saldo se mantiene acumulado (consumed_kg) para responder en O(1).
-- =====================================================================

CREATE TABLE quotas (
    id               VARCHAR(40)    NOT NULL,
    species_id       VARCHAR(40)    NOT NULL,
    -- Zona de aplicación; «TODAS» aplica a cualquier zona (misma convención que las vedas).
    zone             VARCHAR(120)   NOT NULL,
    period_start     DATE           NOT NULL,
    period_end       DATE           NOT NULL,
    total_kg         NUMERIC(12, 2) NOT NULL,
    consumed_kg      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    -- Asignación opcional a una embarcación concreta (esquema de reparto por embarcación).
    vessel_id        VARCHAR(40),
    status           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    -- Último umbral de consumo notificado (0, 80, 95 o 100) para no repetir alertas.
    last_threshold   INTEGER        NOT NULL DEFAULT 0,
    resolution_number VARCHAR(120),
    notes            VARCHAR(400),
    created_by       VARCHAR(40),
    demo             BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_quotas PRIMARY KEY (id),
    CONSTRAINT fk_quotas_species FOREIGN KEY (species_id) REFERENCES species (id),
    CONSTRAINT fk_quotas_vessel FOREIGN KEY (vessel_id) REFERENCES vessels (id),
    CONSTRAINT fk_quotas_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_quotas_total CHECK (total_kg > 0),
    CONSTRAINT ck_quotas_consumed CHECK (consumed_kg >= 0),
    CONSTRAINT ck_quotas_period CHECK (period_end >= period_start),
    CONSTRAINT ck_quotas_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE INDEX ix_quotas_lookup ON quotas (species_id, period_start, period_end);

-- Cada captura recuerda contra qué cuota se descontó (trazabilidad del saldo).
ALTER TABLE catches ADD COLUMN quota_id VARCHAR(40);
ALTER TABLE catches ADD CONSTRAINT fk_catches_quota FOREIGN KEY (quota_id) REFERENCES quotas (id);
