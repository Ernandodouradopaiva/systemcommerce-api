-- V193: Evolução profissional do PurchaseOrder (Prompt 61)
-- Novos status + campos; mapeia status legados PARTIAL→PARTIALLY_RECEIVED mantendo alias via CHECK

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS destination_store_id UUID NULL,
    ADD COLUMN IF NOT EXISTS purchase_quotation_id UUID NULL,
    ADD COLUMN IF NOT EXISTS issued_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS payment_condition VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS carrier_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS freight_modality VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS insurance_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS expense_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS revision_number INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS approval_threshold_amount NUMERIC(18, 2) NULL,
    ADD COLUMN IF NOT EXISTS allow_over_receipt BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_po_destination_store') THEN
        ALTER TABLE purchase_orders
            ADD CONSTRAINT fk_po_destination_store FOREIGN KEY (destination_store_id) REFERENCES stores (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_po_quotation') THEN
        ALTER TABLE purchase_orders
            ADD CONSTRAINT fk_po_quotation FOREIGN KEY (purchase_quotation_id) REFERENCES purchase_quotations (id);
    END IF;
END $$;

UPDATE purchase_orders SET destination_store_id = store_id WHERE destination_store_id IS NULL;
UPDATE purchase_orders SET issued_at = created_at WHERE issued_at IS NULL;

-- Expand status CHECK (drop + recreate)
ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS ck_purchase_orders_status;
-- Map legacy PARTIAL → keep as-is until app maps; add new values including PARTIAL for compat
ALTER TABLE purchase_orders
    ADD CONSTRAINT ck_purchase_orders_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT', 'SENT_TO_SUPPLIER',
        'CONFIRMED_BY_SUPPLIER', 'PARTIAL', 'PARTIALLY_RECEIVED', 'RECEIVED',
        'CLOSED', 'REJECTED', 'CANCELLED'
    ));

ALTER TABLE purchase_order_items
    ADD COLUMN IF NOT EXISTS quantity_cancelled NUMERIC(18, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS expected_date DATE NULL,
    ADD COLUMN IF NOT EXISTS historical_description VARCHAR(300) NULL;

UPDATE purchase_order_items
SET historical_description = COALESCE(historical_description, description)
WHERE historical_description IS NULL;

COMMENT ON COLUMN purchase_orders.purchase_quotation_id IS 'Cotação de origem (Prompt 61)';
COMMENT ON COLUMN purchase_orders.revision_number IS 'Revisão após envio (Prompt 61)';
