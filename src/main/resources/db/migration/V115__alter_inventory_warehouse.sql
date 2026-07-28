-- V115: estoque por depósito (compatível com saldo 1:1 existente)
-- Executa após seeds V105 para preservar dados e backfill.
-- 1) Loja e depósito padrão para migrar saldos atuais
INSERT INTO stores (
    id, code, name, trade_name, timezone, status, active,
    created_at, updated_at, version
) VALUES (
    'c1000000-0000-4000-8000-000000000001',
    'LOJA-01',
    'Loja Principal',
    'SystemCommerce',
    'America/Sao_Paulo',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO warehouses (
    id, store_id, code, name, allows_sale, status, active,
    created_at, updated_at, version
)
SELECT
    'c1000000-0000-4000-8000-000000000002',
    s.id,
    'DEP-01',
    'Depósito Principal',
    TRUE,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
WHERE s.code = 'LOJA-01'
ON CONFLICT (store_id, code) DO NOTHING;

-- 2) Coluna warehouse_id
ALTER TABLE inventory
    ADD COLUMN warehouse_id UUID NULL;

UPDATE inventory i
SET warehouse_id = (
    SELECT w.id
    FROM warehouses w
    JOIN stores s ON s.id = w.store_id
    WHERE s.code = 'LOJA-01' AND w.code = 'DEP-01'
    LIMIT 1
)
WHERE i.warehouse_id IS NULL;

ALTER TABLE inventory
    ALTER COLUMN warehouse_id SET NOT NULL;

ALTER TABLE inventory
    DROP CONSTRAINT uk_inventory_product;

ALTER TABLE inventory
    ADD CONSTRAINT uk_inventory_product_warehouse UNIQUE (product_id, warehouse_id);

ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id);

CREATE INDEX idx_inventory_warehouse_id ON inventory (warehouse_id);

COMMENT ON COLUMN inventory.warehouse_id IS 'Depósito do saldo; migração atribuiu DEP-01 aos saldos existentes';
COMMENT ON TABLE inventory IS 'Saldo atual de estoque por produto e depósito';
