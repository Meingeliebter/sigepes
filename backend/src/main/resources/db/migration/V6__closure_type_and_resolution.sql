-- =====================================================================
-- SIGEPES · RF-05 · Vedas: referencia legal y alcance total/parcial
--   AC-05.4: toda veda debe referenciar su documento legal de respaldo.
--   AC-05.5: distinguir veda total (todos) de parcial (solo subsistencia).
-- =====================================================================

ALTER TABLE closures ADD COLUMN resolution_number VARCHAR(120);
ALTER TABLE closures ADD COLUMN closure_type VARCHAR(20);

UPDATE closures SET resolution_number = 'AUNAP-S/N' WHERE resolution_number IS NULL;
-- Conservador: una veda existente nunca se relaja retroactivamente a "parcial" por defecto.
UPDATE closures SET closure_type = 'TOTAL' WHERE closure_type IS NULL;

ALTER TABLE closures ALTER COLUMN resolution_number SET NOT NULL;
ALTER TABLE closures ALTER COLUMN closure_type SET NOT NULL;

ALTER TABLE closures ADD CONSTRAINT ck_closures_type CHECK (closure_type IN ('TOTAL', 'PARTIAL'));
