-- Consultas de validação multilojas (Prompt 80)
-- Executar ANTES (baseline) e DEPOIS de V173 em cópia da base.

-- 1. Organização / loja / depósito padrão
SELECT 'organizations' AS check_name, COUNT(*) AS cnt FROM organizations WHERE code = 'ORG-DEFAULT';
SELECT 'stores_loja01' AS check_name, COUNT(*) AS cnt FROM stores WHERE code = 'LOJA-01';
SELECT 'warehouses_dep01' AS check_name, COUNT(*) AS cnt
FROM warehouses w JOIN stores s ON s.id = w.store_id
WHERE s.code = 'LOJA-01' AND w.code = 'DEP-01';

-- 2. Dados órfãos (esperado: 0 após V173)
SELECT 'sales_without_store' AS check_name, COUNT(*) AS cnt FROM sales WHERE store_id IS NULL;
SELECT 'sales_without_org' AS check_name, COUNT(*) AS cnt FROM sales WHERE organization_id IS NULL;
SELECT 'inventory_store_mismatch' AS check_name, COUNT(*) AS cnt
FROM inventory i
JOIN warehouses w ON w.id = i.warehouse_id
WHERE i.store_id IS DISTINCT FROM w.store_id;
SELECT 'movements_without_store' AS check_name, COUNT(*) AS cnt
FROM stock_movements WHERE store_id IS NULL;
SELECT 'payments_without_store' AS check_name, COUNT(*) AS cnt
FROM payments WHERE store_id IS NULL;
SELECT 'products_without_org' AS check_name, COUNT(*) AS cnt
FROM products WHERE organization_id IS NULL;
SELECT 'users_without_access' AS check_name, COUNT(*) AS cnt
FROM users u
WHERE u.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM user_store_access x WHERE x.user_id = u.id AND x.status = 'ACTIVE'
  );

-- 3. Integridade de saldos (não devem mudar com migração de vínculo)
SELECT store_id, SUM(quantity) AS on_hand, SUM(quantity_reserved) AS reserved
FROM inventory
GROUP BY store_id
ORDER BY store_id;

-- 4. Totais de vendas por loja (não devem mudar)
SELECT store_id, status, COUNT(*) AS n, SUM(total_amount) AS total
FROM sales
GROUP BY store_id, status
ORDER BY store_id, status;

-- 5. Relatório da migration V173
SELECT check_name, check_status, detail, recorded_at
FROM multistore_migration_report
WHERE migration_version = 'V173'
ORDER BY id;

-- 6. Cobertura store_products
SELECT s.code, COUNT(sp.id) AS products_enabled
FROM stores s
LEFT JOIN store_products sp ON sp.store_id = s.id AND sp.active = TRUE
GROUP BY s.code
ORDER BY s.code;
