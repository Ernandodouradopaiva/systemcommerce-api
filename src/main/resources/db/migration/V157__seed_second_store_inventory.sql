-- V157: segunda loja, depósito e saldos distintos (seed multilojas)

INSERT INTO stores (
    id, organization_id, code, name, trade_name, document, timezone, status, active,
    establishment_type, is_headquarters, allows_sales, allows_pos,
    created_at, updated_at, version
)
SELECT
    'c1000000-0000-4000-8000-000000000011',
    o.id,
    'LOJA-02',
    'Loja Filial Centro',
    'Filial Centro',
    NULL,
    'America/Sao_Paulo',
    'ACTIVE',
    TRUE,
    'BRANCH',
    FALSE,
    TRUE,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM stores s WHERE s.code = 'LOJA-02' AND s.organization_id = o.id);

INSERT INTO warehouses (
    id, store_id, code, name, allows_sale, status, active, created_at, updated_at, version
)
SELECT
    'c1000000-0000-4000-8000-000000000012',
    s.id,
    'DEP-02',
    'Depósito Filial Centro',
    TRUE,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
WHERE s.code = 'LOJA-02'
  AND NOT EXISTS (
      SELECT 1 FROM warehouses w WHERE w.store_id = s.id AND w.code = 'DEP-02'
  );

-- Saldos distintos do mesmo produto (NB-001) nas duas lojas
INSERT INTO inventory (
    id, product_id, warehouse_id, store_id, quantity, quantity_reserved, quantity_blocked,
    quantity_in_transit, minimum_quantity, maximum_quantity, reorder_point,
    active, created_at, updated_at, version
)
SELECT
    'e1000000-0000-4000-8000-000000000201',
    p.id,
    w.id,
    w.store_id,
    50.000,
    0,
    0,
    0,
    COALESCE(p.min_stock, 5),
    200,
    COALESCE(p.min_stock, 5),
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM products p
CROSS JOIN warehouses w
JOIN stores s ON s.id = w.store_id AND s.code = 'LOJA-02' AND w.code = 'DEP-02'
WHERE p.sku = 'NB-001'
  AND NOT EXISTS (
      SELECT 1 FROM inventory i WHERE i.product_id = p.id AND i.warehouse_id = w.id
  );

-- Garante saldo distinto na matriz se já existir (não sobrescreve se já customizado)
UPDATE inventory i
SET quantity = GREATEST(i.quantity, 100.000),
    maximum_quantity = COALESCE(i.maximum_quantity, 500),
    reorder_point = COALESCE(NULLIF(i.reorder_point, 0), i.minimum_quantity, 5)
FROM products p, warehouses w, stores s
WHERE i.product_id = p.id
  AND i.warehouse_id = w.id
  AND w.store_id = s.id
  AND p.sku = 'NB-001'
  AND s.code = 'LOJA-01'
  AND w.code = 'DEP-01';

INSERT INTO inventory (
    id, product_id, warehouse_id, store_id, quantity, quantity_reserved, quantity_blocked,
    quantity_in_transit, minimum_quantity, maximum_quantity, reorder_point,
    active, created_at, updated_at, version
)
SELECT
    'e1000000-0000-4000-8000-000000000202',
    p.id,
    w.id,
    w.store_id,
    25.000,
    0,
    0,
    0,
    10,
    100,
    10,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM products p
CROSS JOIN warehouses w
JOIN stores s ON s.id = w.store_id AND s.code = 'LOJA-02' AND w.code = 'DEP-02'
WHERE p.sku = 'MS-001'
  AND NOT EXISTS (
      SELECT 1 FROM inventory i WHERE i.product_id = p.id AND i.warehouse_id = w.id
  );
