-- V203: Reserva formal de estoque (Prompt 70)
CREATE TABLE stock_reservations (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    warehouse_id        UUID            NOT NULL,
    reservation_number  VARCHAR(40)     NOT NULL,
    origin_type         VARCHAR(40)     NOT NULL,
    origin_id           UUID            NOT NULL,
    origin_number       VARCHAR(40)     NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    expires_at          TIMESTAMPTZ     NULL,
    notes               VARCHAR(2000)   NULL,
    idempotency_key     VARCHAR(80)     NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_stock_reservations PRIMARY KEY (id),
    CONSTRAINT uk_stock_reservations_number UNIQUE (reservation_number),
    CONSTRAINT fk_sr_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_sr_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_sr_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_sr_origin CHECK (origin_type IN (
        'QUOTE', 'SALES_ORDER', 'MARKETPLACE', 'ONLINE', 'SPECIAL_SERVICE'
    )),
    CONSTRAINT ck_sr_status CHECK (status IN (
        'ACTIVE', 'PARTIALLY_CONSUMED', 'CONSUMED', 'RELEASED', 'EXPIRED', 'CANCELLED'
    ))
);

CREATE UNIQUE INDEX uk_sr_idempotency ON stock_reservations (organization_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE stock_reservation_items (
    id                      UUID            NOT NULL,
    stock_reservation_id    UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    line_number             INT             NOT NULL,
    quantity_reserved       NUMERIC(18, 4)  NOT NULL,
    quantity_consumed       NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    quantity_released       NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_stock_reservation_items PRIMARY KEY (id),
    CONSTRAINT uk_sri_line UNIQUE (stock_reservation_id, line_number),
    CONSTRAINT fk_sri_reservation FOREIGN KEY (stock_reservation_id) REFERENCES stock_reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_sri_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_sri_qty CHECK (quantity_reserved > 0 AND quantity_consumed >= 0 AND quantity_released >= 0)
);

CREATE TABLE stock_reservation_status_history (
    id                      UUID            NOT NULL,
    stock_reservation_id    UUID            NOT NULL,
    from_status             VARCHAR(30)     NULL,
    to_status               VARCHAR(30)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_srs_history PRIMARY KEY (id),
    CONSTRAINT fk_srs_reservation FOREIGN KEY (stock_reservation_id) REFERENCES stock_reservations (id) ON DELETE CASCADE
);

CREATE INDEX idx_sr_origin ON stock_reservations (origin_type, origin_id);
CREATE INDEX idx_sr_expires ON stock_reservations (expires_at) WHERE status = 'ACTIVE';

COMMENT ON TABLE stock_reservations IS 'Reserva formal — reduz disponível, não físico (Prompt 70)';
