-- V194: Recebimento tipo GoodsReceipt — workflow draft/inspeção/postagem (Prompt 62)
-- Mantém tabela purchase_receipts (canônico); GoodsReceipt = alias de negócio

ALTER TABLE purchase_receipts
    ADD COLUMN IF NOT EXISTS invoice_series VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS access_key VARCHAR(60) NULL,
    ADD COLUMN IF NOT EXISTS invoice_issued_at DATE NULL,
    ADD COLUMN IF NOT EXISTS carrier_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS posted_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS posted_by UUID NULL,
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(80) NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pr_posted_by') THEN
        ALTER TABLE purchase_receipts
            ADD CONSTRAINT fk_pr_posted_by FOREIGN KEY (posted_by) REFERENCES users (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_receipts_idempotency
    ON purchase_receipts (organization_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE purchase_receipts DROP CONSTRAINT IF EXISTS ck_purchase_receipts_status;
ALTER TABLE purchase_receipts
    ADD CONSTRAINT ck_purchase_receipts_status CHECK (status IN (
        'DRAFT', 'UNDER_INSPECTION', 'PARTIALLY_ACCEPTED', 'ACCEPTED',
        'POSTED_TO_INVENTORY', 'CONFIRMED', 'REJECTED', 'CANCELLED'
    ));

-- CONFIRMED legado = já postado
UPDATE purchase_receipts
SET status = 'POSTED_TO_INVENTORY', posted_at = COALESCE(posted_at, created_at)
WHERE status = 'CONFIRMED';

ALTER TABLE purchase_receipt_items
    ADD COLUMN IF NOT EXISTS quantity_ordered NUMERIC(18, 4) NULL,
    ADD COLUMN IF NOT EXISTS quantity_previously_received NUMERIC(18, 4) NULL,
    ADD COLUMN IF NOT EXISTS quantity_accepted NUMERIC(18, 4) NULL,
    ADD COLUMN IF NOT EXISTS quantity_divergent NUMERIC(18, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(18, 4) NULL,
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS destination_location VARCHAR(120) NULL;

UPDATE purchase_receipt_items
SET quantity_accepted = COALESCE(quantity_accepted, quantity_received)
WHERE quantity_accepted IS NULL;

CREATE TABLE purchase_receipt_status_history (
    id                      UUID            NOT NULL,
    purchase_receipt_id     UUID            NOT NULL,
    from_status             VARCHAR(30)     NULL,
    to_status               VARCHAR(30)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_purchase_receipt_status_history PRIMARY KEY (id),
    CONSTRAINT fk_prh_receipt FOREIGN KEY (purchase_receipt_id) REFERENCES purchase_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_prh_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_prh_receipt ON purchase_receipt_status_history (purchase_receipt_id, changed_at);

CREATE TABLE purchase_receipt_divergences (
    id                      UUID            NOT NULL,
    purchase_receipt_id     UUID            NOT NULL,
    purchase_receipt_item_id UUID           NULL,
    divergence_type         VARCHAR(40)     NOT NULL,
    description             VARCHAR(1000)   NOT NULL,
    quantity                NUMERIC(18, 4)  NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    CONSTRAINT pk_purchase_receipt_divergences PRIMARY KEY (id),
    CONSTRAINT fk_prd_receipt FOREIGN KEY (purchase_receipt_id) REFERENCES purchase_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_prd_item FOREIGN KEY (purchase_receipt_item_id) REFERENCES purchase_receipt_items (id),
    CONSTRAINT ck_prd_type CHECK (divergence_type IN (
        'QUANTITY', 'QUALITY', 'PRODUCT', 'DOCUMENT', 'OTHER'
    ))
);

CREATE TABLE inventory_entry_references (
    id                      UUID            NOT NULL,
    purchase_receipt_id     UUID            NOT NULL,
    inventory_movement_id   UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_inventory_entry_references PRIMARY KEY (id),
    CONSTRAINT uk_ier_movement UNIQUE (inventory_movement_id),
    CONSTRAINT fk_ier_receipt FOREIGN KEY (purchase_receipt_id) REFERENCES purchase_receipts (id),
    CONSTRAINT fk_ier_movement FOREIGN KEY (inventory_movement_id) REFERENCES stock_movements (id),
    CONSTRAINT fk_ier_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_ier_receipt ON inventory_entry_references (purchase_receipt_id);

COMMENT ON TABLE purchase_receipts IS 'Recebimento físico / GoodsReceipt (Prompt 62); postagem gera PURCHASE';
