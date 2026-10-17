-- =====================================================================
-- SIGEPES · MVP v1.1 · RF-09 / HU-09 · Trazabilidad de la cadena de valor
--   RN-18: solo la captura legal (permiso vigente, talla, fuera de veda y
--          dentro de cuota) puede convertirse en lote.
--   AC-09.5: la cadena de custodia es de solo inserción y cada evento guarda
--            el hash del anterior; cualquier alteración rompe la cadena.
-- =====================================================================

CREATE TABLE lots (
    id             VARCHAR(40)    NOT NULL,
    code           VARCHAR(20)    NOT NULL,
    catch_id       VARCHAR(40)    NOT NULL,
    trip_id        VARCHAR(40)    NOT NULL,
    species_id     VARCHAR(40)    NOT NULL,
    vessel_id      VARCHAR(40)    NOT NULL,
    owner_id       VARCHAR(40)    NOT NULL,
    weight_kg      NUMERIC(12, 2) NOT NULL,
    landing_date   DATE           NOT NULL,
    zone           VARCHAR(80)    NOT NULL,
    -- Resultado de las verificaciones de legalidad al crear el lote, como JSON.
    legal_checks   VARCHAR(2000)  NOT NULL,
    demo           BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_lots PRIMARY KEY (id),
    CONSTRAINT uq_lots_code UNIQUE (code),
    CONSTRAINT fk_lots_catch FOREIGN KEY (catch_id) REFERENCES catches (id),
    CONSTRAINT fk_lots_trip FOREIGN KEY (trip_id) REFERENCES fishing_trips (id),
    CONSTRAINT fk_lots_species FOREIGN KEY (species_id) REFERENCES species (id),
    CONSTRAINT fk_lots_vessel FOREIGN KEY (vessel_id) REFERENCES vessels (id),
    CONSTRAINT fk_lots_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_lots_weight CHECK (weight_kg > 0)
);

CREATE INDEX ix_lots_owner ON lots (owner_id, created_at);
CREATE INDEX ix_lots_catch ON lots (catch_id);

CREATE TABLE chain_events (
    id            VARCHAR(40)    NOT NULL,
    lot_id        VARCHAR(40)    NOT NULL,
    sequence_no   INTEGER        NOT NULL,
    event_type    VARCHAR(20)    NOT NULL,
    party_type    VARCHAR(20)    NOT NULL,
    -- Nombre comercial y NIT de quien recibe: visibles para la autoridad, nunca en la página pública.
    party_name    VARCHAR(120)   NOT NULL,
    party_tax_id  VARCHAR(30),
    location      VARCHAR(120)   NOT NULL,
    occurred_on   DATE           NOT NULL,
    recorded_by   VARCHAR(40)    NOT NULL,
    previous_hash VARCHAR(64)    NOT NULL,
    hash          VARCHAR(64)    NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_chain_events PRIMARY KEY (id),
    CONSTRAINT uq_chain_events_seq UNIQUE (lot_id, sequence_no),
    CONSTRAINT fk_chain_events_lot FOREIGN KEY (lot_id) REFERENCES lots (id),
    CONSTRAINT fk_chain_events_user FOREIGN KEY (recorded_by) REFERENCES users (id),
    CONSTRAINT ck_chain_events_type CHECK (event_type IN ('CREATED', 'TRANSFER')),
    CONSTRAINT ck_chain_events_party CHECK (party_type IN ('FISHER', 'BUYER', 'TRADER', 'PROCESSOR', 'EXPORTER'))
);
