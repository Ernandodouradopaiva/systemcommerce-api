-- V165: vendas ERP multilojas — organização, depósito, tabela de preços, sequência por loja

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations (id),
    ADD COLUMN IF NOT EXISTS price_table_id UUID REFERENCES price_tables (id),
    ADD COLUMN IF NOT EXISTS seller_code_snapshot VARCHAR(40),
    ADD COLUMN IF NOT EXISTS seller_name_snapshot VARCHAR(200),
    ADD COLUMN IF NOT EXISTS supervisor_user_id UUID REFERENCES users (id);

-- Backfill organização e loja a partir do depósito/loja existente
UPDATE sales s
SET organization_id = st.organization_id
FROM stores st
WHERE s.store_id = st.id
  AND s.organization_id IS NULL;

UPDATE sales s
SET store_id = w.store_id,
    organization_id = st.organization_id
FROM warehouses w
JOIN stores st ON st.id = w.store_id
WHERE s.warehouse_id = w.id
  AND s.store_id IS NULL;

-- Vendas sem loja → LOJA-01
UPDATE sales s
SET store_id = st.id,
    organization_id = st.organization_id
FROM stores st
WHERE st.code = 'LOJA-01'
  AND s.store_id IS NULL;

-- Depósito padrão da loja quando ausente
UPDATE sales s
SET warehouse_id = w.id
FROM warehouses w
WHERE w.store_id = s.store_id
  AND w.code IN ('DEP-01', 'DEP-02')
  AND s.warehouse_id IS NULL
  AND w.active = TRUE;

-- Preferir depósito principal DEP-01 da loja
UPDATE sales s
SET warehouse_id = w.id
FROM warehouses w
WHERE w.store_id = s.store_id
  AND w.code = 'DEP-01'
  AND s.warehouse_id IS NULL;

ALTER TABLE sales
    ALTER COLUMN store_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_organization ON sales (organization_id);
CREATE INDEX IF NOT EXISTS idx_sales_store_date ON sales (store_id, sale_date DESC);
CREATE INDEX IF NOT EXISTS idx_sales_warehouse ON sales (warehouse_id);

-- Sequência de número por loja
CREATE TABLE IF NOT EXISTS store_sale_sequences (
    store_id UUID PRIMARY KEY REFERENCES stores (id),
    last_value BIGINT NOT NULL DEFAULT 0,
    prefix VARCHAR(10) NOT NULL DEFAULT 'V',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

INSERT INTO store_sale_sequences (store_id, last_value, prefix, updated_at)
SELECT s.id, 0, 'V', NOW() AT TIME ZONE 'UTC'
FROM stores s
ON CONFLICT (store_id) DO NOTHING;

COMMENT ON COLUMN sales.organization_id IS 'Organização da venda (derivada da loja)';
COMMENT ON COLUMN sales.seller_code_snapshot IS 'Código do vendedor no momento da venda';
COMMENT ON COLUMN sales.seller_name_snapshot IS 'Nome do vendedor no momento da venda';
COMMENT ON TABLE store_sale_sequences IS 'Numeração sequencial de vendas por loja';
