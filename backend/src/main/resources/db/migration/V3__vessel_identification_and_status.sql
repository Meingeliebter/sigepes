-- =====================================================================
-- SIGEPES · RF-02 · Identificación de embarcación y estados ampliados
--   AC-02.2: hull type y potencia de motor son obligatorios; capacidad
--            de bodega pasa de opcional a obligatoria.
--   AC-02.4: estados Active / Under maintenance / Decommissioned / Suspended
--            (INACTIVE se renombra a DECOMMISSIONED).
-- =====================================================================

ALTER TABLE vessels ADD COLUMN hull_type VARCHAR(60);
ALTER TABLE vessels ADD COLUMN engine_power NUMERIC(10, 2);

UPDATE vessels SET hull_type = 'Fibra de vidrio' WHERE hull_type IS NULL;
UPDATE vessels SET engine_power = 75.00 WHERE engine_power IS NULL;
UPDATE vessels SET capacity_tons = 5.00 WHERE capacity_tons IS NULL;

ALTER TABLE vessels ALTER COLUMN hull_type SET NOT NULL;
ALTER TABLE vessels ALTER COLUMN engine_power SET NOT NULL;
ALTER TABLE vessels ALTER COLUMN capacity_tons SET NOT NULL;

ALTER TABLE vessels ADD CONSTRAINT ck_vessels_engine_power CHECK (engine_power > 0);

UPDATE vessels SET status = 'DECOMMISSIONED' WHERE status = 'INACTIVE';

ALTER TABLE vessels DROP CONSTRAINT ck_vessels_status;
ALTER TABLE vessels ADD CONSTRAINT ck_vessels_status
    CHECK (status IN ('ACTIVE', 'UNDER_MAINTENANCE', 'DECOMMISSIONED', 'SUSPENDED'));
