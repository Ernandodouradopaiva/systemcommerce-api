-- V161: transferências de estoque entre lojas/depósitos

CREATE TABLE IF NOT EXISTS stock_transfers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    number VARCHAR(40) NOT NULL,
    origin_store_id UUID NOT NULL REFERENCES stores (id),
    origin_warehouse_id UUID NOT NULL REFERENCES warehouses (id),
    destination_store_id UUID NOT NULL REFERENCES stores (id),
    destination_warehouse_id UUID NOT NULL REFERENCES warehouses (id),
    requester_id UUID REFERENCES users (id),
    approver_id UUID REFERENCES users (id),
    dispatcher_id UUID REFERENCES users (id),
    receiver_id UUID REFERENCES users (id),
    requested_at TIMESTAMPTZ,
    dispatched_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    observation VARCHAR(1000),
    reason VARCHAR(500),
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_transfers_org_number UNIQUE (organization_id, number),
    CONSTRAINT uk_stock_transfers_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT ck_stock_transfers_status CHECK (status IN (
        'DRAFT', 'REQUESTED', 'APPROVED', 'PREPARING', 'DISPATCHED',
        'IN_TRANSIT', 'PARTIALLY_RECEIVED', 'RECEIVED', 'REJECTED', 'CANCELLED'
    )),
    CONSTRAINT ck_stock_transfers_origin_dest CHECK (
        origin_warehouse_id <> destination_warehouse_id
    )
);

CREATE TABLE IF NOT EXISTS stock_transfer_items (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES stock_transfers (id),
    product_id UUID NOT NULL REFERENCES products (id),
    quantity_requested NUMERIC(19, 3) NOT NULL,
    quantity_approved NUMERIC(19, 3),
    quantity_dispatched NUMERIC(19, 3) NOT NULL DEFAULT 0,
    quantity_received NUMERIC(19, 3) NOT NULL DEFAULT 0,
    quantity_divergent NUMERIC(19, 3) NOT NULL DEFAULT 0,
    observation VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_transfer_items_product UNIQUE (transfer_id, product_id),
    CONSTRAINT ck_stock_transfer_items_qty_positive CHECK (quantity_requested > 0)
);

CREATE TABLE IF NOT EXISTS stock_transfer_status_history (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES stock_transfers (id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by UUID REFERENCES users (id),
    reason VARCHAR(500),
    observation VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID
);

CREATE TABLE IF NOT EXISTS stock_transfer_receipts (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES stock_transfers (id),
    item_id UUID NOT NULL REFERENCES stock_transfer_items (id),
    quantity_received NUMERIC(19, 3) NOT NULL,
    quantity_expected NUMERIC(19, 3),
    divergence_quantity NUMERIC(19, 3) NOT NULL DEFAULT 0,
    divergence_reason VARCHAR(500),
    received_by UUID REFERENCES users (id),
    received_at TIMESTAMPTZ NOT NULL,
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_transfer_receipts_idempotency UNIQUE (transfer_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_stock_transfers_origin ON stock_transfers (origin_store_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_transfers_dest ON stock_transfers (destination_store_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_transfers_status ON stock_transfers (status);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_items_transfer ON stock_transfer_items (transfer_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_history_transfer ON stock_transfer_status_history (transfer_id);

-- Tipos de movimentação de transferência
-- (enum Java; coluna type já é VARCHAR)

COMMENT ON TABLE stock_transfers IS 'Transferência oficial entre depósitos/lojas (não usar ajuste)';
