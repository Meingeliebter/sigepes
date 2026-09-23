-- =====================================================================
-- SIGEPES · RF-03 · Bitácora electrónica: campos obligatorios adicionales
--   AC-03.2: puerto de origen, ubicación GPS de desembarco y conteo
--            individual (quantity) obligatorios.
-- =====================================================================

ALTER TABLE fishing_trips ADD COLUMN origin_port VARCHAR(120);
ALTER TABLE fishing_trips ADD COLUMN landing_latitude NUMERIC(9, 6);
ALTER TABLE fishing_trips ADD COLUMN landing_longitude NUMERIC(9, 6);

UPDATE fishing_trips SET origin_port = (
    SELECT port FROM vessels WHERE vessels.id = fishing_trips.vessel_id
) WHERE origin_port IS NULL;

ALTER TABLE fishing_trips ALTER COLUMN origin_port SET NOT NULL;

UPDATE catches SET quantity = 1 WHERE quantity IS NULL;

ALTER TABLE catches ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE catches DROP CONSTRAINT ck_catches_quantity;
ALTER TABLE catches ADD CONSTRAINT ck_catches_quantity CHECK (quantity > 0);
