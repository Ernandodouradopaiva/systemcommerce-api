-- V200: Tipos de depósito e endereçamento físico (Prompt 67)
ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS warehouse_type VARCHAR(30) NOT NULL DEFAULT 'SALE',
    ADD COLUMN IF NOT EXISTS central BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS virtual_warehouse BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS blocked_for_movement BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE warehouses DROP CONSTRAINT IF EXISTS ck_warehouses_type;
ALTER TABLE warehouses ADD CONSTRAINT ck_warehouses_type CHECK (warehouse_type IN (
    'CENTRAL', 'SALE', 'RETURN', 'DAMAGE', 'QUARANTINE', 'VIRTUAL', 'OTHER'
));

CREATE TABLE warehouse_zones (
    id                  UUID            NOT NULL,
    warehouse_id        UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_warehouse_zones PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_zones UNIQUE (warehouse_id, code),
    CONSTRAINT fk_wz_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE
);

CREATE TABLE warehouse_aisles (
    id                  UUID            NOT NULL,
    zone_id             UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_warehouse_aisles PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_aisles UNIQUE (zone_id, code),
    CONSTRAINT fk_wa_zone FOREIGN KEY (zone_id) REFERENCES warehouse_zones (id) ON DELETE CASCADE
);

CREATE TABLE warehouse_racks (
    id                  UUID            NOT NULL,
    aisle_id            UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_warehouse_racks PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_racks UNIQUE (aisle_id, code),
    CONSTRAINT fk_wr_aisle FOREIGN KEY (aisle_id) REFERENCES warehouse_aisles (id) ON DELETE CASCADE
);

CREATE TABLE warehouse_shelves (
    id                  UUID            NOT NULL,
    rack_id             UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_warehouse_shelves PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_shelves UNIQUE (rack_id, code),
    CONSTRAINT fk_ws_rack FOREIGN KEY (rack_id) REFERENCES warehouse_racks (id) ON DELETE CASCADE
);

CREATE TABLE storage_locations (
    id                  UUID            NOT NULL,
    warehouse_id        UUID            NOT NULL,
    zone_id             UUID            NULL,
    aisle_id            UUID            NULL,
    rack_id             UUID            NULL,
    shelf_id            UUID            NULL,
    code                VARCHAR(80)     NOT NULL,
    barcode             VARCHAR(80)     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    track_balance       BOOLEAN         NOT NULL DEFAULT FALSE,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_storage_locations PRIMARY KEY (id),
    CONSTRAINT uk_storage_locations UNIQUE (warehouse_id, code),
    CONSTRAINT fk_sl_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,
    CONSTRAINT fk_sl_zone FOREIGN KEY (zone_id) REFERENCES warehouse_zones (id),
    CONSTRAINT fk_sl_aisle FOREIGN KEY (aisle_id) REFERENCES warehouse_aisles (id),
    CONSTRAINT fk_sl_rack FOREIGN KEY (rack_id) REFERENCES warehouse_racks (id),
    CONSTRAINT fk_sl_shelf FOREIGN KEY (shelf_id) REFERENCES warehouse_shelves (id)
);

CREATE TABLE product_storage_locations (
    id                      UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    storage_location_id     UUID            NOT NULL,
    preferred               BOOLEAN         NOT NULL DEFAULT FALSE,
    min_quantity            NUMERIC(18, 4)  NULL,
    max_quantity            NUMERIC(18, 4)  NULL,
    quantity_at_location    NUMERIC(18, 4)  NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_storage_locations PRIMARY KEY (id),
    CONSTRAINT uk_psl UNIQUE (product_id, storage_location_id),
    CONSTRAINT fk_psl_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_psl_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id)
);

CREATE INDEX idx_storage_locations_wh ON storage_locations (warehouse_id);
CREATE INDEX idx_psl_product ON product_storage_locations (product_id);
