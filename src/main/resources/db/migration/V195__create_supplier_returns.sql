-- V195: Devolução ao fornecedor (Prompt 63)
CREATE TABLE store_supplier_return_sequences (
    store_id    UUID            NOT NULL,
    last_value  BIGINT          NOT NULL DEFAULT 0,
    prefix      VARCHAR(10)     NOT NULL DEFAULT 'DF',
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_supplier_return_sequences PRIMARY KEY (store_id),
    CONSTRAINT fk_ssr_seq_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE supplier_returns (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    supplier_id             UUID            NOT NULL,
    purchase_order_id       UUID            NULL,
    purchase_receipt_id     UUID            NULL,
    return_number           VARCHAR(40)     NOT NULL,
    reason                  VARCHAR(40)     NOT NULL,
    reason_notes            VARCHAR(2000)   NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    origin_type             VARCHAR(40)     NOT NULL,
    dispatched_at           TIMESTAMPTZ     NULL,
    completed_at            TIMESTAMPTZ     NULL,
    notes                   VARCHAR(2000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_returns PRIMARY KEY (id),
    CONSTRAINT uk_supplier_returns_number UNIQUE (return_number),
    CONSTRAINT fk_sr_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_sr_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_sr_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_sr_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_sr_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_sr_receipt FOREIGN KEY (purchase_receipt_id) REFERENCES purchase_receipts (id),
    CONSTRAINT ck_sr_reason CHECK (reason IN (
        'WRONG_PRODUCT', 'EXCESS_QUANTITY', 'DAMAGE', 'DEFECT',
        'EXPIRY', 'DIVERGENCE', 'CANCELLATION', 'OTHER'
    )),
    CONSTRAINT ck_sr_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'DISPATCHED',
        'COMPLETED', 'REJECTED', 'CANCELLED'
    )),
    CONSTRAINT ck_sr_origin CHECK (origin_type IN (
        'RECEIPT', 'PURCHASE_ORDER', 'BATCH', 'INSPECTION', 'EXISTING_STOCK'
    ))
);

CREATE INDEX idx_sr_store_status ON supplier_returns (store_id, status);
CREATE INDEX idx_sr_supplier ON supplier_returns (supplier_id);

CREATE TABLE supplier_return_items (
    id                      UUID            NOT NULL,
    supplier_return_id      UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    purchase_order_item_id  UUID            NULL,
    purchase_receipt_item_id UUID           NULL,
    line_number             INT             NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    unit_cost               NUMERIC(18, 4)  NULL,
    batch_code              VARCHAR(80)     NULL,
    expiry_date             DATE            NULL,
    serial_number           VARCHAR(120)    NULL,
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_return_items PRIMARY KEY (id),
    CONSTRAINT uk_sr_items_line UNIQUE (supplier_return_id, line_number),
    CONSTRAINT fk_sri_return FOREIGN KEY (supplier_return_id) REFERENCES supplier_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_sri_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_sri_poi FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items (id),
    CONSTRAINT fk_sri_pri FOREIGN KEY (purchase_receipt_item_id) REFERENCES purchase_receipt_items (id),
    CONSTRAINT ck_sri_qty CHECK (quantity > 0)
);

CREATE TABLE supplier_return_status_history (
    id                      UUID            NOT NULL,
    supplier_return_id      UUID            NOT NULL,
    from_status             VARCHAR(30)     NULL,
    to_status               VARCHAR(30)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_sr_status_history PRIMARY KEY (id),
    CONSTRAINT fk_srh_return FOREIGN KEY (supplier_return_id) REFERENCES supplier_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_srh_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_srh_return ON supplier_return_status_history (supplier_return_id, changed_at);

COMMENT ON TABLE supplier_returns IS 'Devolução ao fornecedor — COMPLETED gera SUPPLIER_RETURN (Prompt 63)';
