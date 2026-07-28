-- V24: ampliação do módulo de vendas (Prompt 14)
CREATE SEQUENCE IF NOT EXISTS sale_number_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS sale_number VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS sale_date TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS surcharge_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS freight_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE sales SET sale_number = 'LEGACY-' || REPLACE(id::text, '-', '') WHERE sale_number IS NULL;
UPDATE sales SET sale_date = created_at WHERE sale_date IS NULL;

ALTER TABLE sales
    ALTER COLUMN sale_number SET NOT NULL,
    ALTER COLUMN sale_date SET NOT NULL;

ALTER TABLE sales DROP CONSTRAINT IF EXISTS uk_sales_sale_number;
ALTER TABLE sales ADD CONSTRAINT uk_sales_sale_number UNIQUE (sale_number);

ALTER TABLE sales DROP CONSTRAINT IF EXISTS ck_sales_status;
ALTER TABLE sales
    ADD CONSTRAINT ck_sales_status CHECK (status IN (
        'DRAFT', 'CONFIRMED', 'PAID', 'PARTIALLY_PAID', 'CANCELLED'
    ));

ALTER TABLE sales DROP CONSTRAINT IF EXISTS ck_sales_discount_lte_subtotal;
ALTER TABLE sales
    ADD CONSTRAINT ck_sales_discount_lte_subtotal CHECK (discount_amount <= subtotal),
    ADD CONSTRAINT ck_sales_surcharge_non_negative CHECK (surcharge_amount >= 0),
    ADD CONSTRAINT ck_sales_freight_non_negative CHECK (freight_amount >= 0);

-- Rascunho pode existir sem cliente até a definição explícita
ALTER TABLE sales ALTER COLUMN customer_id DROP NOT NULL;

ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS description VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS line_subtotal NUMERIC(19, 2) NULL;

UPDATE sale_items
SET line_subtotal = ROUND(quantity * unit_price, 2)
WHERE line_subtotal IS NULL;

ALTER TABLE sale_items
    ALTER COLUMN line_subtotal SET NOT NULL;

ALTER TABLE sale_items
    ADD CONSTRAINT ck_sale_items_line_subtotal_non_negative CHECK (line_subtotal >= 0),
    ADD CONSTRAINT ck_sale_items_discount_lte_subtotal CHECK (discount_amount <= line_subtotal);

ALTER TABLE sale_status_history DROP CONSTRAINT IF EXISTS ck_sale_status_history_from;
ALTER TABLE sale_status_history DROP CONSTRAINT IF EXISTS ck_sale_status_history_to;
ALTER TABLE sale_status_history
    ADD CONSTRAINT ck_sale_status_history_from CHECK (
        from_status IS NULL OR from_status IN ('DRAFT', 'CONFIRMED', 'PAID', 'PARTIALLY_PAID', 'CANCELLED')
    ),
    ADD CONSTRAINT ck_sale_status_history_to CHECK (
        to_status IN ('DRAFT', 'CONFIRMED', 'PAID', 'PARTIALLY_PAID', 'CANCELLED')
    );

CREATE INDEX IF NOT EXISTS idx_sales_sale_number ON sales (sale_number);
CREATE INDEX IF NOT EXISTS idx_sales_sale_date ON sales (sale_date);

COMMENT ON COLUMN sales.sale_number IS 'Número único gerado no backend';
COMMENT ON COLUMN sales.surcharge_amount IS 'Acréscimo calculado/validado na API';
COMMENT ON COLUMN sales.freight_amount IS 'Frete calculado/validado na API';
COMMENT ON COLUMN sale_items.line_subtotal IS 'quantity * unit_price (antes do desconto do item)';
COMMENT ON COLUMN sale_items.line_total IS 'line_subtotal - discount_amount (oficial da API)';
