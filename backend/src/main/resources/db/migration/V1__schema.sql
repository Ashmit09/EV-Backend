-- VoltX schema (MySQL 8). Owned by Flyway; Hibernate never alters it.
-- Enum-like columns are VARCHAR so new values never require an ALTER on ENUM types.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE vehicles (
    id                         BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id                   BIGINT        NOT NULL,
    vehicle_type               VARCHAR(20)   NOT NULL,
    manufacturer               VARCHAR(60)   NOT NULL,
    model                      VARCHAR(60)   NOT NULL,
    registration_number        VARCHAR(20)   NOT NULL,
    battery_capacity_kwh       DECIMAL(6, 2) NOT NULL,
    max_charging_power_kw      DECIMAL(6, 2) NOT NULL,
    current_battery_percentage INT           NOT NULL,
    created_at                 DATETIME(6)   NOT NULL,
    updated_at                 DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vehicles_registration UNIQUE (registration_number),
    CONSTRAINT fk_vehicles_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_vehicles_battery CHECK (current_battery_percentage BETWEEN 0 AND 100)
);
CREATE INDEX idx_vehicles_owner ON vehicles (owner_id);

CREATE TABLE vehicle_connectors (
    vehicle_id     BIGINT      NOT NULL,
    connector_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (vehicle_id, connector_type),
    CONSTRAINT fk_vehicle_connectors_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE
);

CREATE TABLE charging_stations (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120)   NOT NULL,
    address       VARCHAR(255)   NOT NULL,
    city          VARCHAR(80)    NOT NULL,
    latitude      DECIMAL(9, 6)  NOT NULL,
    longitude     DECIMAL(9, 6)  NOT NULL,
    opening_time  TIME           NOT NULL,
    closing_time  TIME           NOT NULL,
    open_24_hours BOOLEAN        NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)    NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stations_name_city UNIQUE (name, city)
);
CREATE INDEX idx_stations_city ON charging_stations (city);

CREATE TABLE charging_units (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    station_id     BIGINT        NOT NULL,
    charger_code   VARCHAR(30)   NOT NULL,
    connector_type VARCHAR(20)   NOT NULL,
    max_power_kw   DECIMAL(6, 2) NOT NULL,
    price_per_kwh  DECIMAL(8, 2) NOT NULL,
    pricing_plan   VARCHAR(20)   NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_units_code UNIQUE (charger_code),
    CONSTRAINT fk_units_station FOREIGN KEY (station_id) REFERENCES charging_stations (id) ON DELETE CASCADE
);
CREATE INDEX idx_units_station_status ON charging_units (station_id, status);

CREATE TABLE reservations (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    vehicle_id          BIGINT       NOT NULL,
    charging_unit_id    BIGINT       NOT NULL,
    start_time          DATETIME(6)  NOT NULL,
    end_time            DATETIME(6)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    cancellation_reason VARCHAR(255),
    cancelled_at        DATETIME(6),
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservations_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT fk_reservations_unit FOREIGN KEY (charging_unit_id) REFERENCES charging_units (id),
    CONSTRAINT ck_reservations_window CHECK (end_time > start_time)
);
CREATE INDEX idx_reservations_unit_window ON reservations (charging_unit_id, start_time, end_time);
CREATE INDEX idx_reservations_user_status ON reservations (user_id, status);

CREATE TABLE charging_sessions (
    id                         BIGINT        NOT NULL AUTO_INCREMENT,
    reservation_id             BIGINT,
    user_id                    BIGINT        NOT NULL,
    vehicle_id                 BIGINT        NOT NULL,
    charging_unit_id           BIGINT        NOT NULL,
    start_time                 DATETIME(6)   NOT NULL,
    end_time                   DATETIME(6),
    initial_battery_percentage DECIMAL(5, 2) NOT NULL,
    target_battery_percentage  DECIMAL(5, 2) NOT NULL,
    final_battery_percentage   DECIMAL(5, 2),
    energy_consumed_kwh        DECIMAL(8, 3),
    simulated_duration_minutes INT,
    effective_power_kw         DECIMAL(6, 2) NOT NULL,
    price_per_kwh              DECIMAL(8, 2) NOT NULL,
    pricing_plan               VARCHAR(20)   NOT NULL,
    cost                       DECIMAL(10, 2),
    status                     VARCHAR(20)   NOT NULL,
    created_at                 DATETIME(6)   NOT NULL,
    updated_at                 DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sessions_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_sessions_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT fk_sessions_unit FOREIGN KEY (charging_unit_id) REFERENCES charging_units (id)
);
CREATE INDEX idx_sessions_user_status ON charging_sessions (user_id, status);
CREATE INDEX idx_sessions_status ON charging_sessions (status);

CREATE TABLE invoices (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_number    VARCHAR(30)    NOT NULL,
    session_id        BIGINT         NOT NULL,
    user_id           BIGINT         NOT NULL,
    energy_kwh        DECIMAL(8, 3)  NOT NULL,
    rate_per_kwh      DECIMAL(8, 2)  NOT NULL,
    pricing_plan      VARCHAR(20)    NOT NULL,
    energy_cost       DECIMAL(10, 2) NOT NULL,
    adjustment_label  VARCHAR(80),
    adjustment_amount DECIMAL(10, 2) NOT NULL,
    subtotal          DECIMAL(10, 2) NOT NULL,
    tax_rate          DECIMAL(4, 3)  NOT NULL,
    tax_amount        DECIMAL(10, 2) NOT NULL,
    total_amount      DECIMAL(10, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    issued_at         DATETIME(6)    NOT NULL,
    paid_at           DATETIME(6),
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_session UNIQUE (session_id),
    CONSTRAINT fk_invoices_session FOREIGN KEY (session_id) REFERENCES charging_sessions (id),
    CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_invoices_user ON invoices (user_id);
CREATE INDEX idx_invoices_issued ON invoices (issued_at);

CREATE TABLE payments (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_id      BIGINT         NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    method          VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    transaction_ref VARCHAR(40)    NOT NULL,
    message         VARCHAR(255),
    processed_at    DATETIME(6)    NOT NULL,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_ref UNIQUE (transaction_ref),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);
CREATE INDEX idx_payments_invoice ON payments (invoice_id);

CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    actor_email VARCHAR(150) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(500),
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
