-- =====================================================================
-- SIGEPES - MVP v1.0 "Núcleo de control y legalidad"
-- Esquema base. SQL portable (PostgreSQL y H2) a propósito:
--   * identificadores VARCHAR generados por la aplicación (con prefijo legible)
--   * enumerados como VARCHAR + CHECK (no se usan tipos ENUM nativos)
--   * timestamps TIMESTAMP, fechas DATE
-- Cobertura: RF-01, RF-02, RF-03, RF-05, RF-06, RF-08, RF-20
-- =====================================================================

-- ---------------------------------------------------------------------
-- RF-01 · Usuarios, identidad y roles
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              VARCHAR(40)  NOT NULL,
    national_id     VARCHAR(12)  NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(180) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    demo            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    -- RN-01: un documento de identidad no puede registrarse más de una vez
    CONSTRAINT uq_users_national_id UNIQUE (national_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('FISHER', 'SHIPOWNER', 'INSPECTOR', 'ANALYST', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE INDEX ix_users_role ON users (role);

-- ---------------------------------------------------------------------
-- RF-02 · Artes de pesca (catálogo)
-- ---------------------------------------------------------------------
CREATE TABLE fishing_gears (
    id              VARCHAR(40)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(400),
    -- RN-04: las artes destructivas/prohibidas no pueden asociarse a embarcaciones
    destructive     BOOLEAN      NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    demo            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_fishing_gears PRIMARY KEY (id),
    CONSTRAINT uq_fishing_gears_name UNIQUE (name),
    CONSTRAINT ck_fishing_gears_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE'))
);

-- ---------------------------------------------------------------------
-- RF-02 · Embarcaciones
-- ---------------------------------------------------------------------
CREATE TABLE vessels (
    id              VARCHAR(40)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    registration    VARCHAR(60)  NOT NULL,
    port            VARCHAR(120) NOT NULL,
    vessel_type     VARCHAR(60),
    capacity_tons   NUMERIC(10, 2),
    status          VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    owner_id        VARCHAR(40)  NOT NULL,
    demo            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vessels PRIMARY KEY (id),
    CONSTRAINT uq_vessels_registration UNIQUE (registration),
    CONSTRAINT fk_vessels_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_vessels_status CHECK (status IN ('ACTIVE', 'UNDER_MAINTENANCE', 'INACTIVE')),
    CONSTRAINT ck_vessels_capacity CHECK (capacity_tons IS NULL OR capacity_tons > 0)
);

CREATE INDEX ix_vessels_owner ON vessels (owner_id);

-- Relación N:M embarcación <-> arte de pesca
CREATE TABLE vessel_gears (
    vessel_id       VARCHAR(40) NOT NULL,
    gear_id         VARCHAR(40) NOT NULL,
    CONSTRAINT pk_vessel_gears PRIMARY KEY (vessel_id, gear_id),
    CONSTRAINT fk_vessel_gears_vessel FOREIGN KEY (vessel_id) REFERENCES vessels (id) ON DELETE CASCADE,
    CONSTRAINT fk_vessel_gears_gear FOREIGN KEY (gear_id) REFERENCES fishing_gears (id)
);

CREATE INDEX ix_vessel_gears_gear ON vessel_gears (gear_id);

-- ---------------------------------------------------------------------
-- RF-06 · Especies y tallas mínimas legales
-- ---------------------------------------------------------------------
CREATE TABLE species (
    id              VARCHAR(40)  NOT NULL,
    common_name     VARCHAR(120) NOT NULL,
    scientific_name VARCHAR(160) NOT NULL,
    -- RN-12: talla mínima legal de captura/comercialización
    min_size_cm     NUMERIC(6, 2) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    demo            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_species PRIMARY KEY (id),
    CONSTRAINT uq_species_common_name UNIQUE (common_name),
    CONSTRAINT ck_species_status CHECK (status IN ('ACTIVE', 'CLOSED', 'INACTIVE')),
    CONSTRAINT ck_species_min_size CHECK (min_size_cm > 0)
);

-- ---------------------------------------------------------------------
-- RF-08 · Permisos y licencias de pesca
-- ---------------------------------------------------------------------
CREATE TABLE permits (
    id                 VARCHAR(40)  NOT NULL,
    number             VARCHAR(40)  NOT NULL,
    permit_type        VARCHAR(80)  NOT NULL,
    holder             VARCHAR(160) NOT NULL,
    owner_id           VARCHAR(40)  NOT NULL,
    issued_at          DATE         NOT NULL,
    expires_at         DATE         NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reason             VARCHAR(400),
    verification_code  VARCHAR(40)  NOT NULL,
    reviewed_by        VARCHAR(40),
    reviewed_at        TIMESTAMP,
    demo               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_permits PRIMARY KEY (id),
    CONSTRAINT uq_permits_number UNIQUE (number),
    CONSTRAINT uq_permits_verification_code UNIQUE (verification_code),
    CONSTRAINT fk_permits_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_permits_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_permits_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED', 'EXPIRED')),
    CONSTRAINT ck_permits_dates CHECK (expires_at >= issued_at)
);

CREATE INDEX ix_permits_owner ON permits (owner_id);
CREATE INDEX ix_permits_status ON permits (status);
CREATE INDEX ix_permits_expires ON permits (expires_at);

-- ---------------------------------------------------------------------
-- RF-05 · Vedas y temporadas
-- ---------------------------------------------------------------------
CREATE TABLE closures (
    id              VARCHAR(40)  NOT NULL,
    species_id      VARCHAR(40)  NOT NULL,
    zone            VARCHAR(120) NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    -- SCHEDULED = programada, CLOSED = veda vigente, FINISHED = terminada/levantada
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    reason          VARCHAR(400),
    notes           VARCHAR(400),
    created_by      VARCHAR(40),
    demo            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_closures PRIMARY KEY (id),
    CONSTRAINT fk_closures_species FOREIGN KEY (species_id) REFERENCES species (id),
    CONSTRAINT fk_closures_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_closures_status CHECK (status IN ('SCHEDULED', 'CLOSED', 'FINISHED')),
    CONSTRAINT ck_closures_dates CHECK (end_date >= start_date)
);

CREATE INDEX ix_closures_species ON closures (species_id);
CREATE INDEX ix_closures_window ON closures (start_date, end_date);

-- ---------------------------------------------------------------------
-- RF-03 · Bitácora de faenas y capturas
-- ---------------------------------------------------------------------
CREATE TABLE fishing_trips (
    id               VARCHAR(40)  NOT NULL,
    owner_id         VARCHAR(40)  NOT NULL,
    vessel_id        VARCHAR(40)  NOT NULL,
    gear_id          VARCHAR(40),
    permit_id        VARCHAR(40),
    zone             VARCHAR(120) NOT NULL,
    trip_date        DATE         NOT NULL,
    departure_time   TIME,
    arrival_time     TIME,
    status           VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    -- RN-06: no se cierra una faena sin capturas ni declaración de captura nula
    null_declaration BOOLEAN      NOT NULL DEFAULT FALSE,
    notes            VARCHAR(600),
    closed_at        TIMESTAMP,
    demo             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_fishing_trips PRIMARY KEY (id),
    CONSTRAINT fk_trips_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_trips_vessel FOREIGN KEY (vessel_id) REFERENCES vessels (id),
    CONSTRAINT fk_trips_gear FOREIGN KEY (gear_id) REFERENCES fishing_gears (id),
    CONSTRAINT fk_trips_permit FOREIGN KEY (permit_id) REFERENCES permits (id),
    CONSTRAINT ck_trips_status CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'CLOSED', 'CANCELLED'))
);

CREATE INDEX ix_trips_owner ON fishing_trips (owner_id);
CREATE INDEX ix_trips_vessel ON fishing_trips (vessel_id);
CREATE INDEX ix_trips_date ON fishing_trips (trip_date);

CREATE TABLE catches (
    id               VARCHAR(40)  NOT NULL,
    trip_id          VARCHAR(40)  NOT NULL,
    species_id       VARCHAR(40)  NOT NULL,
    quantity         INTEGER,
    weight_kg        NUMERIC(10, 2) NOT NULL,
    average_size_cm  NUMERIC(6, 2)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_catches PRIMARY KEY (id),
    CONSTRAINT fk_catches_trip FOREIGN KEY (trip_id) REFERENCES fishing_trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_catches_species FOREIGN KEY (species_id) REFERENCES species (id),
    CONSTRAINT ck_catches_weight CHECK (weight_kg > 0),
    CONSTRAINT ck_catches_size CHECK (average_size_cm > 0),
    CONSTRAINT ck_catches_quantity CHECK (quantity IS NULL OR quantity > 0)
);

CREATE INDEX ix_catches_trip ON catches (trip_id);
CREATE INDEX ix_catches_species ON catches (species_id);

-- ---------------------------------------------------------------------
-- RF-20 · Bitácora de auditoría (append-only; RN-40)
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id            VARCHAR(40)  NOT NULL,
    occurred_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id      VARCHAR(40),
    actor_label   VARCHAR(160) NOT NULL,
    action        VARCHAR(60)  NOT NULL,
    module        VARCHAR(40)  NOT NULL,
    entity        VARCHAR(80)  NOT NULL,
    entity_id     VARCHAR(60),
    detail        VARCHAR(600),
    ip_address    VARCHAR(60),
    before_state  VARCHAR(2000),
    after_state   VARCHAR(2000),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX ix_audit_occurred_at ON audit_logs (occurred_at);
CREATE INDEX ix_audit_actor ON audit_logs (actor_id);
CREATE INDEX ix_audit_module ON audit_logs (module);
