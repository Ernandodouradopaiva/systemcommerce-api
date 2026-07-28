-- V173: consolidação idempotente da migração de dados para multilojas (Prompt 80)
-- Não altera totais, saldos nem valores históricos. Apenas vincula contexto ausente.
-- Flyway já garante execução única; statements usam IF NOT EXISTS / NOT EXISTS para segurança.

-- ========== 1. Garantir organização padrão ==========
INSERT INTO organizations (
    id, code, legal_name, trade_name, document,
    email, phone, default_timezone, currency, status, active,
    created_at, updated_at, version
)
SELECT
    'b1000000-0000-4000-8000-000000000001',
    'ORG-DEFAULT',
    'SystemCommerce Organização Padrão LTDA',
    'SystemCommerce',
    NULL,
    'contato@systemcommerce.local',
    NULL,
    'America/Sao_Paulo',
    'BRL',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE code = 'ORG-DEFAULT');

UPDATE stores
SET organization_id = (SELECT id FROM organizations WHERE code = 'ORG-DEFAULT' LIMIT 1)
WHERE organization_id IS NULL;

-- ========== 2. Produtos → organização (cadastro global com escopo org) ==========
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations (id);

UPDATE products
SET organization_id = (SELECT id FROM organizations WHERE code = 'ORG-DEFAULT' LIMIT 1)
WHERE organization_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_organization ON products (organization_id);

COMMENT ON COLUMN products.organization_id IS 'Organização dona do cadastro canônico do produto';

-- ========== 3. Store products — cobertura completa (sem duplicar) ==========
INSERT INTO store_products (
    id, store_id, product_id, status, allows_sale, allows_pos_sale, allows_erp_sale,
    allow_negative_stock, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    s.id,
    p.id,
    'ACTIVE',
    TRUE,
    TRUE,
    TRUE,
    COALESCE(p.allow_negative_stock, FALSE),
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
CROSS JOIN products p
WHERE s.active = TRUE
  AND p.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM store_products sp
      WHERE sp.store_id = s.id AND sp.product_id = p.id
  );

-- ========== 4. Inventory — sincronizar store_id do depósito ==========
UPDATE inventory i
SET store_id = w.store_id
FROM warehouses w
WHERE i.warehouse_id = w.id
  AND (i.store_id IS NULL OR i.store_id <> w.store_id);

-- ========== 5. Stock movements — denormalizar loja (preserva valores) ==========
ALTER TABLE stock_movements
    ADD COLUMN IF NOT EXISTS store_id UUID;

UPDATE stock_movements sm
SET store_id = w.store_id
FROM warehouses w
WHERE sm.warehouse_id = w.id
  AND (sm.store_id IS NULL OR sm.store_id <> w.store_id);

-- Fallback LOJA-01 se ainda houver movimento sem depósito/loja
UPDATE stock_movements sm
SET store_id = st.id
FROM stores st
WHERE st.code = 'LOJA-01'
  AND sm.store_id IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM stock_movements WHERE store_id IS NULL
    ) THEN
        RAISE NOTICE 'V173: ainda existem stock_movements sem store_id (revisar manualmente)';
    ELSE
        ALTER TABLE stock_movements ALTER COLUMN store_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_stock_movements_store'
    ) THEN
        ALTER TABLE stock_movements
            ADD CONSTRAINT fk_stock_movements_store
                FOREIGN KEY (store_id) REFERENCES stores (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_stock_movements_store_id ON stock_movements (store_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_store_created ON stock_movements (store_id, created_at DESC);

COMMENT ON COLUMN stock_movements.store_id IS 'Loja do depósito no momento da movimentação (denormalizada; valores históricos intactos)';

-- ========== 6. Sales — completar org/loja/depósito ==========
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

UPDATE sales s
SET store_id = st.id,
    organization_id = st.organization_id
FROM stores st
WHERE st.code = 'LOJA-01'
  AND s.store_id IS NULL;

UPDATE sales s
SET warehouse_id = w.id
FROM warehouses w
WHERE w.store_id = s.store_id
  AND w.code = 'DEP-01'
  AND s.warehouse_id IS NULL
  AND w.active = TRUE;

-- ========== 7. Payments — vincular loja da venda (valores intactos) ==========
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS store_id UUID;

UPDATE payments p
SET store_id = s.store_id
FROM sales s
WHERE p.sale_id = s.id
  AND (p.store_id IS NULL OR p.store_id <> s.store_id);

UPDATE payments p
SET store_id = st.id
FROM stores st
WHERE st.code = 'LOJA-01'
  AND p.store_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM payments WHERE store_id IS NULL) THEN
        ALTER TABLE payments ALTER COLUMN store_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_store'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT fk_payments_store
                FOREIGN KEY (store_id) REFERENCES stores (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_payments_store_id ON payments (store_id);

COMMENT ON COLUMN payments.store_id IS 'Loja da venda associada (denormalizada para consultas/relatórios)';

-- ========== 8. Terminais e sessões — loja obrigatória já existe; só sanitiza órfãos ==========
-- (nenhuma alteração de valores; apenas garante FK coerente se houver lixo de ambiente legado)

-- ========== 9. Acessos de usuário — quem não tem nenhuma loja recebe LOJA-01 ==========
INSERT INTO user_store_access (
    id, user_id, store_id, start_date, end_date, default_store, access_type, status,
    granted_by_id, reason, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    u.id,
    st.id,
    CURRENT_DATE,
    NULL,
    TRUE,
    'PERMANENT',
    'ACTIVE',
    u.id,
    'V173 migração multilojas — acesso inicial LOJA-01',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM users u
CROSS JOIN stores st
WHERE st.code = 'LOJA-01'
  AND u.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM user_store_access x
      WHERE x.user_id = u.id AND x.status = 'ACTIVE'
  );

-- ========== 10. Profissionais — usuários com papel operacional sem employee ==========
-- Cria employee mínimo vinculado quando usuário ativo ainda não tem vínculo (sem duplicar matrícula)
INSERT INTO employees (
    id, organization_id, registration_number, name, social_name, job_title, status,
    user_id, can_sell, admission_date, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    o.id,
    'MIG-' || UPPER(LEFT(REPLACE(u.id::text, '-', ''), 8)),
    COALESCE(NULLIF(TRIM(u.name), ''), u.login),
    NULL,
    'Colaborador',
    'ACTIVE',
    u.id,
    FALSE,
    CURRENT_DATE,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM users u
CROSS JOIN organizations o
WHERE o.code = 'ORG-DEFAULT'
  AND u.active = TRUE
  AND u.login <> 'admin'
  AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.user_id = u.id)
  AND NOT EXISTS (
      SELECT 1 FROM employees e2
      WHERE e2.organization_id = o.id
        AND e2.registration_number = 'MIG-' || UPPER(LEFT(REPLACE(u.id::text, '-', ''), 8))
  );

-- Lotação permanente principal LOJA-01 para profissionais sem lotação ativa
INSERT INTO employee_store_assignments (
    id, employee_id, store_id, assignment_type, start_date, end_date,
    primary_assignment, store_role, status, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    e.id,
    st.id,
    'PERMANENT',
    CURRENT_DATE,
    NULL,
    TRUE,
    'Colaborador',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM employees e
CROSS JOIN stores st
WHERE st.code = 'LOJA-01'
  AND e.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM employee_store_assignments a
      WHERE a.employee_id = e.id AND a.status = 'ACTIVE'
  );

-- ========== 11. Vendedores — autorizar loja principal se perfil ativo sem assignment ==========
INSERT INTO seller_store_assignments (
    id, seller_profile_id, store_id, start_date, end_date,
    primary_assignment, temporary_assignment, allows_register_sale,
    max_discount_percent, status, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    sp.id,
    st.id,
    CURRENT_DATE,
    NULL,
    TRUE,
    FALSE,
    TRUE,
    sp.max_discount_percent,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM seller_profiles sp
CROSS JOIN stores st
WHERE st.code = 'LOJA-01'
  AND sp.status = 'ACTIVE'
  AND sp.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM seller_store_assignments ssa
      WHERE ssa.seller_profile_id = sp.id AND ssa.status = 'ACTIVE'
  );

-- ========== 12. Auditoria — backfill organization_id a partir da loja ==========
UPDATE audit_logs a
SET organization_id = st.organization_id
FROM stores st
WHERE a.store_id = st.id
  AND a.organization_id IS NULL;

-- ========== 13. Relatório de migração (metadados) ==========
CREATE TABLE IF NOT EXISTS multistore_migration_report (
    id BIGSERIAL PRIMARY KEY,
    migration_version VARCHAR(20) NOT NULL,
    check_name VARCHAR(120) NOT NULL,
    check_status VARCHAR(20) NOT NULL,
    detail TEXT,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

DELETE FROM multistore_migration_report WHERE migration_version = 'V173';

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'organizations',
       CASE WHEN COUNT(*) >= 1 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM organizations WHERE code = 'ORG-DEFAULT';

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'default_store',
       CASE WHEN COUNT(*) >= 1 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM stores WHERE code = 'LOJA-01';

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'sales_without_store',
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM sales WHERE store_id IS NULL;

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'inventory_store_mismatch',
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM inventory i
JOIN warehouses w ON w.id = i.warehouse_id
WHERE i.store_id IS DISTINCT FROM w.store_id;

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'stock_movements_without_store',
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM stock_movements WHERE store_id IS NULL;

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'payments_without_store',
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM payments WHERE store_id IS NULL;

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'products_without_organization',
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM products WHERE organization_id IS NULL;

INSERT INTO multistore_migration_report (migration_version, check_name, check_status, detail)
SELECT 'V173', 'users_without_store_access',
       CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'FAIL' END,
       'count=' || COUNT(*)
FROM users u
WHERE u.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM user_store_access x
      WHERE x.user_id = u.id AND x.status = 'ACTIVE'
  );

COMMENT ON TABLE multistore_migration_report IS 'Resultado das validações pós-migração multilojas (V173+)';
