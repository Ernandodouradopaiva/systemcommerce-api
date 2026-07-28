-- V210: Controle de lotes e validade (Prompt 76)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS requires_batch BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fefo_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE product_batches (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    supplier_id             UUID            NULL,
    batch_code              VARCHAR(80)     NOT NULL,
    manufactured_at         DATE            NULL,
    expires_at              DATE            NULL,
    received_at             TIMESTAMPTZ     NULL,
    purchase_receipt_id     UUID            NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_batches PRIMARY KEY (id),
    CONSTRAINT uk_product_batches UNIQUE (organization_id, product_id, batch_code),
    CONSTRAINT fk_pb_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pb_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_pb_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_pb_receipt FOREIGN KEY (purchase_receipt_id) REFERENCES purchase_receipts (id),
    CONSTRAINT ck_pb_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED', 'DEPLETED'))
);

CREATE TABLE batch_inventories (
    id                      UUID            NOT NULL,
    product_batch_id        UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    storage_location_id     UUID            NULL,
    quantity                NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    quantity_reserved       NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_batch_inventories PRIMARY KEY (id),
    CONSTRAINT uk_batch_inventories UNIQUE (product_batch_id, warehouse_id, storage_location_id),
    CONSTRAINT fk_bi_batch FOREIGN KEY (product_batch_id) REFERENCES product_batches (id),
    CONSTRAINT fk_bi_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_bi_loc FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT ck_bi_qty CHECK (quantity >= 0 AND quantity_reserved >= 0)
);

CREATE TABLE batch_movements (
    id                      UUID            NOT NULL,
    product_batch_id        UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    inventory_movement_id   UUID            NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    direction               VARCHAR(10)     NOT NULL,
    origin_type             VARCHAR(40)     NULL,
    origin_id               UUID            NULL,
    balance_after           NUMERIC(18, 4)  NULL,
    occurred_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    performed_by            UUID            NULL,
    CONSTRAINT pk_batch_movements PRIMARY KEY (id),
    CONSTRAINT fk_bm_batch FOREIGN KEY (product_batch_id) REFERENCES product_batches (id),
    CONSTRAINT fk_bm_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_bm_movement FOREIGN KEY (inventory_movement_id) REFERENCES stock_movements (id),
    CONSTRAINT ck_bm_dir CHECK (direction IN ('IN', 'OUT'))
);

CREATE TABLE batch_reservations (
    id                      UUID            NOT NULL,
    product_batch_id        UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    origin_type             VARCHAR(40)     NOT NULL,
    origin_id               UUID            NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    released_at             TIMESTAMPTZ     NULL,
    CONSTRAINT pk_batch_reservations PRIMARY KEY (id),
    CONSTRAINT fk_br_batch FOREIGN KEY (product_batch_id) REFERENCES product_batches (id),
    CONSTRAINT fk_br_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_br_qty CHECK (quantity > 0),
    CONSTRAINT ck_br_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'CANCELLED'))
);

CREATE INDEX idx_product_batches_expiry ON product_batches (expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_batch_inventories_wh ON batch_inventories (warehouse_id);

COMMENT ON TABLE product_batches IS 'Lotes com validade — FEFO configurável (Prompt 76)';
