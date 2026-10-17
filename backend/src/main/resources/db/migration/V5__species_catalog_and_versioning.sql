-- =====================================================================
-- SIGEPES · RF-06 · Catálogo de especies: atributos, región y versionado
--   AC-06.2: método de medición e imagen de referencia obligatorios.
--   AC-06.4: historial de versiones + snapshot de talla mínima en cada
--            captura, para no alterar el registro histórico de faenas.
--   AC-06.5: región (Pacífico / Caribe / Ambas) para filtrar el catálogo.
-- =====================================================================

ALTER TABLE species ADD COLUMN measurement_method VARCHAR(20);
ALTER TABLE species ADD COLUMN region VARCHAR(20);
ALTER TABLE species ADD COLUMN image_url VARCHAR(500);

UPDATE species SET measurement_method = 'TOTAL_LENGTH' WHERE measurement_method IS NULL;
UPDATE species SET region = 'AMBAS' WHERE region IS NULL;
UPDATE species SET image_url = 'https://placehold.co/400x300?text=Especie' WHERE image_url IS NULL;

ALTER TABLE species ALTER COLUMN measurement_method SET NOT NULL;
ALTER TABLE species ALTER COLUMN region SET NOT NULL;
ALTER TABLE species ALTER COLUMN image_url SET NOT NULL;

ALTER TABLE species ADD CONSTRAINT ck_species_measurement_method
    CHECK (measurement_method IN ('TOTAL_LENGTH', 'FORK_LENGTH'));
ALTER TABLE species ADD CONSTRAINT ck_species_region CHECK (region IN ('PACIFICO', 'CARIBE', 'AMBAS'));

-- ---------------------------------------------------------------------
-- AC-06.4 · Historial de versiones del catálogo (append-only)
-- ---------------------------------------------------------------------
CREATE TABLE species_versions (
    id                  VARCHAR(40)  NOT NULL,
    species_id          VARCHAR(40)  NOT NULL,
    version             INTEGER      NOT NULL,
    common_name         VARCHAR(120) NOT NULL,
    scientific_name     VARCHAR(160) NOT NULL,
    min_size_cm         NUMERIC(6, 2) NOT NULL,
    measurement_method  VARCHAR(20)  NOT NULL,
    region              VARCHAR(20)  NOT NULL,
    image_url           VARCHAR(500) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    changed_by          VARCHAR(40),
    changed_by_label    VARCHAR(160) NOT NULL,
    changed_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_species_versions PRIMARY KEY (id),
    CONSTRAINT fk_species_versions_species FOREIGN KEY (species_id) REFERENCES species (id)
);

CREATE INDEX ix_species_versions_species ON species_versions (species_id, version);

-- ---------------------------------------------------------------------
-- AC-06.4 · Snapshot de talla mínima en cada captura ya registrada
-- ---------------------------------------------------------------------
ALTER TABLE catches ADD COLUMN min_size_cm_at_capture NUMERIC(6, 2);

UPDATE catches SET min_size_cm_at_capture = (
    SELECT min_size_cm FROM species WHERE species.id = catches.species_id
) WHERE min_size_cm_at_capture IS NULL;

ALTER TABLE catches ALTER COLUMN min_size_cm_at_capture SET NOT NULL;
