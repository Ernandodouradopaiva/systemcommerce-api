-- V211: Controle por número de série (Prompt 77)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS requires_serial BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE product_serial_numbers (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    serial_number           VARCHAR(120)    NOT NULL,
    product_batch_id        UUID            NULL,
    warehouse_id            UUID            NULL,
    storage_location_id     UUID            NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    purchase_receipt_id     UUID            NULL,
    sale_id                 UUID            NULL,
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_serial_numbers PRIMARY KEY (id),
    CONSTRAINT uk_psn_org_serial UNIQUE (organization_id, serial_number),
    CONSTRAINT fk_psn_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_psn_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_psn_batch FOREIGN KEY (product_batch_id) REFERENCES product_batches (id),
    CONSTRAINT fk_psn_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_psn_loc FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT fk_psn_receipt FOREIGN KEY (purchase_receipt_id) REFERENCES purchase_receipts (id),
    CONSTRAINT fk_psn_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT ck_psn_status CHECK (status IN (
        'AVAILABLE', 'RESERVED', 'SOLD', 'RETURNED', 'DEFECTIVE', 'IN_TRANSIT', 'BLOCKED'
    ))
);

CREATE TABLE serial_number_movements (
    id                      UUID            NOT NULL,
    product_serial_id       UUID            NOT NULL,
    from_status             VARCHAR(20)     NULL,
    to_status               VARCHAR(20)     NOT NULL,
    origin_type             VARCHAR(40)     NULL,
    origin_id               UUID            NULL,
    warehouse_id            UUID            NULL,
    notes                   VARCHAR(1000)   NULL,
    occurred_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    performed_by            UUID            NULL,
    CONSTRAINT pk_serial_number_movements PRIMARY KEY (id),
    CONSTRAINT fk_snm_serial FOREIGN KEY (product_serial_id) REFERENCES product_serial_numbers (id) ON DELETE CASCADE
);

CREATE TABLE serial_number_reservations (
    id                      UUID            NOT NULL,
    product_serial_id       UUID            NOT NULL,
    origin_type             VARCHAR(40)     NOT NULL,
    origin_id               UUID            NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    released_at             TIMESTAMPTZ     NULL,
    CONSTRAINT pk_serial_number_reservations PRIMARY KEY (id),
    CONSTRAINT fk_snr_serial FOREIGN KEY (product_serial_id) REFERENCES product_serial_numbers (id),
    CONSTRAINT ck_snr_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'CANCELLED'))
);

CREATE TABLE serial_number_status_history (
    id                      UUID            NOT NULL,
    product_serial_id       UUID            NOT NULL,
    from_status             VARCHAR(20)     NULL,
    to_status               VARCHAR(20)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_sns_history PRIMARY KEY (id),
    CONSTRAINT fk_snsh_serial FOREIGN KEY (product_serial_id) REFERENCES product_serial_numbers (id) ON DELETE CASCADE
);

CREATE INDEX idx_psn_product_status ON product_serial_numbers (product_id, status);
CREATE INDEX idx_psn_serial ON product_serial_numbers (serial_number);

COMMENT ON TABLE product_serial_numbers IS 'Rastreabilidade individual — série única por organização (Prompt 77)';
