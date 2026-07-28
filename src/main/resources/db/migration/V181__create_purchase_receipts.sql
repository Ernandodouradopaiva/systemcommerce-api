-- V181: recebimento de mercadorias (Prompt 61)
CREATE TABLE purchase_receipts (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    warehouse_id        UUID            NOT NULL,
    purchase_order_id   UUID            NOT NULL,
    supplier_id         UUID            NOT NULL,
    receipt_number      VARCHAR(40)     NOT NULL,
    receipt_date        DATE            NOT NULL,
    invoice_number      VARCHAR(80)     NULL,
    notes               VARCHAR(2000)   NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMED',
    received_by_user_id UUID            NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_receipts PRIMARY KEY (id),
    CONSTRAINT uk_purchase_receipts_number UNIQUE (receipt_number),
    CONSTRAINT fk_purchase_receipts_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_purchase_receipts_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_purchase_receipts_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_purchase_receipts_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_purchase_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_purchase_receipts_user FOREIGN KEY (received_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_purchase_receipts_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE TABLE purchase_receipt_items (
    id                      UUID            NOT NULL,
    purchase_receipt_id     UUID            NOT NULL,
    purchase_order_item_id  UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    quantity_received       NUMERIC(18, 4)  NOT NULL,
    quantity_rejected       NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    batch_code              VARCHAR(60)     NULL,
    expiry_date             DATE            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_receipt_items PRIMARY KEY (id),
    CONSTRAINT fk_purchase_receipt_items_receipt FOREIGN KEY (purchase_receipt_id) REFERENCES purchase_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_receipt_items_poi FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items (id),
    CONSTRAINT fk_purchase_receipt_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_purchase_receipt_items_qty CHECK (quantity_received >= 0 AND quantity_rejected >= 0)
);

CREATE INDEX idx_purchase_receipts_order ON purchase_receipts (purchase_order_id);
CREATE INDEX idx_purchase_receipts_supplier ON purchase_receipts (supplier_id);
CREATE INDEX idx_purchase_receipt_items_receipt ON purchase_receipt_items (purchase_receipt_id);

COMMENT ON TABLE purchase_receipts IS 'Recebimento físico (Prompt 61) — total/parcial/múltiplas entregas; só qty recebida entra no estoque';
