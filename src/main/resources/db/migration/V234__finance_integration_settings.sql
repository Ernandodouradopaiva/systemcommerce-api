-- Prompt 102–104: configurações de geração financeira e vínculo CashMovement ↔ holder

ALTER TABLE finance_generation_settings
    ADD COLUMN IF NOT EXISTS payable_generation_mode VARCHAR(40) NOT NULL DEFAULT 'ON_RECEIPT',
    ADD COLUMN IF NOT EXISTS freight_handling VARCHAR(40) NOT NULL DEFAULT 'INCORPORATED',
    ADD COLUMN IF NOT EXISTS segregate_taxes BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS generate_payable_on_order_approved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS generate_payable_on_invoice_entry BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS settle_pos_cash BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS settle_pos_pix BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS settle_pos_card_immediately BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pos_pix_holder_id UUID NULL,
    ADD COLUMN IF NOT EXISTS pos_card_acquirer_holder_id UUID NULL;

COMMENT ON COLUMN finance_generation_settings.payable_generation_mode IS
    'ON_ORDER_APPROVED | ON_RECEIPT | ON_INVOICE_ENTRY | MANUAL — default ON_RECEIPT (após aceite/post do recebimento)';
COMMENT ON COLUMN finance_generation_settings.freight_handling IS
    'INCORPORATED | SEPARATE';

-- Alinha flag legado com o modo padrão recomendado
UPDATE finance_generation_settings
SET generate_payable_on_receipt = TRUE,
    payable_generation_mode = COALESCE(payable_generation_mode, 'ON_RECEIPT')
WHERE TRUE;

ALTER TABLE cash_movements
    ADD COLUMN IF NOT EXISTS financial_holder_movement_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_cash_movements_holder_movement'
    ) THEN
        ALTER TABLE cash_movements
            ADD CONSTRAINT fk_cash_movements_holder_movement
            FOREIGN KEY (financial_holder_movement_id)
            REFERENCES financial_holder_movements (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cash_movements_holder_movement
    ON cash_movements (financial_holder_movement_id)
    WHERE financial_holder_movement_id IS NOT NULL;
