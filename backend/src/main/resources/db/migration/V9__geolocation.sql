-- =====================================================================
-- SIGEPES · MVP v1.1 · RF-04 / HU-04 · Geolocalización de la zona de pesca
--   RN-08: entrar a un AMP o zona en veda dispara alerta y evento de control.
--   AC-04.5: la caja envolvente (min/max) permite descartar zonas sin
--            evaluar el polígono completo (prefiltro de rendimiento).
-- =====================================================================

CREATE TABLE restricted_zones (
    id                VARCHAR(40)   NOT NULL,
    name              VARCHAR(160)  NOT NULL,
    zone_type         VARCHAR(20)   NOT NULL,
    -- Polígono como JSON [[lat, lon], ...] (sin repetir el primer vértice al final).
    polygon           VARCHAR(12000) NOT NULL,
    min_lat           NUMERIC(9, 6) NOT NULL,
    max_lat           NUMERIC(9, 6) NOT NULL,
    min_lon           NUMERIC(9, 6) NOT NULL,
    max_lon           NUMERIC(9, 6) NOT NULL,
    resolution_number VARCHAR(120)  NOT NULL,
    description       VARCHAR(400),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(40),
    demo              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_restricted_zones PRIMARY KEY (id),
    CONSTRAINT fk_restricted_zones_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_restricted_zones_type CHECK (zone_type IN ('CLOSURE_ZONE', 'MPA', 'OTHER'))
);

CREATE INDEX ix_restricted_zones_bbox ON restricted_zones (active, min_lat, max_lat, min_lon, max_lon);

CREATE TABLE trip_positions (
    id              VARCHAR(40)   NOT NULL,
    trip_id         VARCHAR(40)   NOT NULL,
    latitude        NUMERIC(9, 6) NOT NULL,
    longitude       NUMERIC(9, 6) NOT NULL,
    accuracy_m      NUMERIC(8, 2),
    recorded_at     TIMESTAMP     NOT NULL,
    -- Zona restringida en la que cayó la posición (null si estaba fuera de toda zona).
    inside_zone_id  VARCHAR(40),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_trip_positions PRIMARY KEY (id),
    CONSTRAINT fk_trip_positions_trip FOREIGN KEY (trip_id) REFERENCES fishing_trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_positions_zone FOREIGN KEY (inside_zone_id) REFERENCES restricted_zones (id),
    CONSTRAINT ck_trip_positions_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_trip_positions_lon CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX ix_trip_positions_trip ON trip_positions (trip_id, recorded_at);

-- Evento de control para los inspectores (RN-08).
CREATE TABLE zone_events (
    id            VARCHAR(40)   NOT NULL,
    trip_id       VARCHAR(40)   NOT NULL,
    zone_id       VARCHAR(40)   NOT NULL,
    position_id   VARCHAR(40)   NOT NULL,
    owner_id      VARCHAR(40)   NOT NULL,
    latitude      NUMERIC(9, 6) NOT NULL,
    longitude     NUMERIC(9, 6) NOT NULL,
    occurred_at   TIMESTAMP     NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_zone_events PRIMARY KEY (id),
    CONSTRAINT fk_zone_events_trip FOREIGN KEY (trip_id) REFERENCES fishing_trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_zone_events_zone FOREIGN KEY (zone_id) REFERENCES restricted_zones (id),
    CONSTRAINT fk_zone_events_position FOREIGN KEY (position_id) REFERENCES trip_positions (id) ON DELETE CASCADE,
    CONSTRAINT fk_zone_events_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_zone_events_status CHECK (status IN ('OPEN', 'REVIEWED'))
);

CREATE INDEX ix_zone_events_occurred ON zone_events (occurred_at);
