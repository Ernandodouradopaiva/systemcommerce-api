-- V180: pedidos de compra (Prompt 60)
CREATE TABLE store_purchase_order_sequences (
    store_id    UUID            NOT NULL,
    last_value  BIGINT          NOT NULL DEFAULT 0,
    prefix      VARCHAR(10)     NOT NULL DEFAULT 'C',
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_purchase_order_sequences PRIMARY KEY (store_id),
    CONSTRAINT fk_store_purchase_order_sequences_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE purchase_orders (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    warehouse_id        UUID            NOT NULL,
    supplier_id         UUID            NOT NULL,
    buyer_user_id       UUID            NULL,
    order_number        VARCHAR(40)     NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    expected_date       DATE            NULL,
    notes               VARCHAR(2000)   NULL,
    subtotal_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    freight_amount      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    total_amount        NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_orders PRIMARY KEY (id),
    CONSTRAINT uk_purchase_orders_number UNIQUE (order_number),
    CONSTRAINT fk_purchase_orders_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_purchase_orders_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_purchase_orders_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_purchase_orders_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_purchase_orders_buyer FOREIGN KEY (buyer_user_id) REFERENCES users (id),
    CONSTRAINT ck_purchase_orders_status CHECK (status IN (
        'DRAFT', 'SENT', 'APPROVED', 'PARTIAL', 'RECEIVED', 'CANCELLED'
    )),
    CONSTRAINT ck_purchase_orders_amounts CHECK (
        subtotal_amount >= 0 AND discount_amount >= 0 AND freight_amount >= 0
        AND tax_amount >= 0 AND total_amount >= 0
    )
);

CREATE TABLE purchase_order_items (
    id                      UUID            NOT NULL,
    purchase_order_id       UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    line_number             INT             NOT NULL,
    description             VARCHAR(300)    NULL,
    quantity_ordered        NUMERIC(18, 4)  NOT NULL,
    quantity_received       NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    unit_cost               NUMERIC(18, 4)  NOT NULL,
    discount_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    tax_amount              NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    line_total              NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_order_items PRIMARY KEY (id),
    CONSTRAINT uk_purchase_order_items_line UNIQUE (purchase_order_id, line_number),
    CONSTRAINT fk_purchase_order_items_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_purchase_order_items_qty CHECK (quantity_ordered > 0 AND quantity_received >= 0)
);

CREATE TABLE purchase_order_status_history (
    id                  UUID            NOT NULL,
    purchase_order_id   UUID            NOT NULL,
    from_status         VARCHAR(30)     NULL,
    to_status           VARCHAR(30)     NOT NULL,
    notes               VARCHAR(1000)   NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    CONSTRAINT pk_purchase_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_purchase_order_status_history_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_purchase_orders_store ON purchase_orders (store_id);
CREATE INDEX idx_purchase_orders_supplier ON purchase_orders (supplier_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders (status);
CREATE INDEX idx_purchase_order_items_order ON purchase_order_items (purchase_order_id);

COMMENT ON TABLE purchase_orders IS 'Pedidos de compra (Prompt 60) — fluxo até recebimento e estoque';
