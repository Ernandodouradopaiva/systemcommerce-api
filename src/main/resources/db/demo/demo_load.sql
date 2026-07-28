-- =============================================================================
-- SystemCommerce — carga DEMO para testes (opt-in, fora do Flyway versionado)
-- Idempotente: pode reexecutar; usa UUIDs d900… / usuários a900…
-- Senha dos usuários demo: Demo@123 (BCrypt)
-- NÃO executar em produção.
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 0) Constantes / refs
-- ---------------------------------------------------------------------------
-- Admin: a0000000-0000-4000-8000-000000000001
-- Org:   b1000000-0000-4000-8000-000000000001
-- LOJA-01 / DEP-01 / TERM-01: c100…0001 / 0002 / 0003
-- LOJA-02 / DEP-02: c100…0011 / 0012
-- Produtos seed: d100…0001..0004
-- Clientes seed: e100…0001..0004

-- ---------------------------------------------------------------------------
-- 1) Role CASHIER + vínculo de permissões PDV (cópia do SELLER + caixa)
-- ---------------------------------------------------------------------------
INSERT INTO roles (id, code, name, description, active, created_at, updated_at, version)
VALUES (
    'b1000000-0000-4000-8000-000000000005',
    'CASHIER',
    'Operador de Caixa',
    'Perfil demo PDV',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CASHIER'
  AND (
      p.code LIKE 'POS_%'
      OR p.code LIKE 'CASH_%'
      OR p.code IN (
          'SALE_READ', 'SALE_CREATE', 'SALE_CONFIRM',
          'PRODUCT_READ', 'CUSTOMER_READ', 'INVENTORY_READ',
          'PAYMENT_READ', 'PAYMENT_CREATE', 'DASHBOARD_READ'
      )
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------------
-- 2) Usuários demo (senha Demo@123)
-- ---------------------------------------------------------------------------
INSERT INTO users (
    id, name, email, login, password_hash, active, status,
    failed_login_attempts, created_at, updated_at, version
) VALUES
(
    'a9000000-0000-4000-8000-000000000001',
    'Gerente Demo',
    'gerente@systemcommerce.local',
    'gerente',
    '$2b$10$GXi49S8gG5wN28XbSCHyzudR/HxPalV/7.Nq8.DFmQIV1iq7OCiKS',
    TRUE, 'ACTIVE', 0, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
),
(
    'a9000000-0000-4000-8000-000000000002',
    'Caixa Demo',
    'caixa@systemcommerce.local',
    'caixa',
    '$2b$10$GXi49S8gG5wN28XbSCHyzudR/HxPalV/7.Nq8.DFmQIV1iq7OCiKS',
    TRUE, 'ACTIVE', 0, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
),
(
    'a9000000-0000-4000-8000-000000000003',
    'Vendedor Demo',
    'vendedor@systemcommerce.local',
    'vendedor',
    '$2b$10$GXi49S8gG5wN28XbSCHyzudR/HxPalV/7.Nq8.DFmQIV1iq7OCiKS',
    TRUE, 'ACTIVE', 0, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
),
(
    'a9000000-0000-4000-8000-000000000004',
    'Estoquista Demo',
    'estoque@systemcommerce.local',
    'estoque',
    '$2b$10$GXi49S8gG5wN28XbSCHyzudR/HxPalV/7.Nq8.DFmQIV1iq7OCiKS',
    TRUE, 'ACTIVE', 0, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, NOW() AT TIME ZONE 'UTC'
FROM (VALUES
    ('gerente@systemcommerce.local', 'MANAGER'),
    ('caixa@systemcommerce.local', 'CASHIER'),
    ('vendedor@systemcommerce.local', 'SELLER'),
    ('estoque@systemcommerce.local', 'STOCK_KEEPER'),
    ('admin@systemcommerce.local', 'ADMIN')
) AS m(email, role_code)
JOIN users u ON u.email = m.email
JOIN roles r ON r.code = m.role_code
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- Acesso às duas lojas
INSERT INTO user_store_access (
    id, user_id, store_id, start_date, default_store, access_type, status,
    active, created_at, updated_at, version
)
SELECT
    ('a9100000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 100), 12, '0'))::uuid,
    u.id,
    s.id,
    CURRENT_DATE,
    (s.code = 'LOJA-01'),
    'PERMANENT',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM users u
CROSS JOIN stores s
WHERE u.email IN (
    'admin@systemcommerce.local',
    'gerente@systemcommerce.local',
    'caixa@systemcommerce.local',
    'vendedor@systemcommerce.local',
    'estoque@systemcommerce.local'
)
  AND s.code IN ('LOJA-01', 'LOJA-02')
  AND NOT EXISTS (
      SELECT 1 FROM user_store_access usa
      WHERE usa.user_id = u.id AND usa.store_id = s.id
  );

-- ---------------------------------------------------------------------------
-- 3) Terminal PDV LOJA-02
-- ---------------------------------------------------------------------------
INSERT INTO pos_terminals (
    id, store_id, warehouse_id, code, name, terminal_number,
    status, station_identifier, print_model, active,
    created_at, updated_at, version
)
SELECT
    'c1000000-0000-4000-8000-000000000013',
    s.id,
    w.id,
    'TERM-02',
    'Terminal PDV Filial',
    1,
    'ACTIVE',
    'STATION-02',
    'NONE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
JOIN warehouses w ON w.store_id = s.id AND w.code = 'DEP-02'
WHERE s.code = 'LOJA-02'
  AND NOT EXISTS (
      SELECT 1 FROM pos_terminals t WHERE t.store_id = s.id AND t.code = 'TERM-02'
  );

-- ---------------------------------------------------------------------------
-- 4) Categorias e produtos extras
-- ---------------------------------------------------------------------------
INSERT INTO categories (id, name, description, active, status, created_at, updated_at, version)
VALUES
(
    'c1000000-0000-4000-8000-000000000010',
    'Acessórios',
    'Acessórios diversos',
    TRUE, 'ACTIVE', NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (
    id, sku, internal_code, name, description, category_id,
    unit_price, cost_price, barcode, unit_of_measure, min_stock,
    allow_negative_stock, status, active, organization_id,
    created_at, updated_at, version
)
SELECT
    v.id, v.sku, v.sku, v.name, v.description, c.id,
    v.unit_price, v.cost_price, v.barcode, 'UN', v.min_stock,
    FALSE, 'ACTIVE', TRUE, o.id,
    NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o
CROSS JOIN (VALUES
    ('d9000000-0000-4000-8000-000000000001'::uuid, 'TEC-001', 'Teclado USB', 'Teclado padrão ABNT2', '7891000100011', 89.90, 45.00, 5.000),
    ('d9000000-0000-4000-8000-000000000002'::uuid, 'HD-001', 'Headset com microfone', 'Headset office', '7891000100028', 159.90, 80.00, 3.000),
    ('d9000000-0000-4000-8000-000000000003'::uuid, 'CAB-USB', 'Cabo USB-C 1m', 'Cabo dados/carga', '7891000100035', 39.90, 12.00, 20.000),
    ('d9000000-0000-4000-8000-000000000004'::uuid, 'SSD-512', 'SSD 512GB', 'SSD SATA 2.5"', '7891000100042', 349.90, 220.00, 2.000),
    ('d9000000-0000-4000-8000-000000000005'::uuid, 'MOU-WL', 'Mouse sem fio', 'Mouse wireless', '7891000100059', 79.90, 35.00, 8.000)
) AS v(id, sku, name, description, barcode, unit_price, cost_price, min_stock)
JOIN categories c ON c.name = 'Acessórios'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.sku = v.sku);

-- Habilita todos os produtos ativos nas duas lojas
INSERT INTO store_products (
    id, store_id, product_id, status, allows_sale, allows_pos_sale, allows_erp_sale,
    allow_negative_stock, active, created_at, updated_at, version
)
SELECT
    ('d9100000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 200), 12, '0'))::uuid,
    s.id,
    p.id,
    'ACTIVE',
    TRUE, TRUE, TRUE,
    FALSE,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
CROSS JOIN products p
WHERE s.code IN ('LOJA-01', 'LOJA-02')
  AND p.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM store_products sp
      WHERE sp.store_id = s.id AND sp.product_id = p.id
  );

-- Estoque completo DEP-01 e DEP-02
INSERT INTO inventory (
    id, product_id, warehouse_id, store_id, quantity, quantity_reserved,
    quantity_blocked, quantity_in_transit, minimum_quantity, maximum_quantity,
    reorder_point, active, created_at, updated_at, version
)
SELECT
    ('f9000000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 300), 12, '0'))::uuid,
    p.id,
    w.id,
    w.store_id,
    CASE WHEN s.code = 'LOJA-01' THEN 80.000 ELSE 40.000 END,
    0, 0, 0,
    COALESCE(p.min_stock, 5),
    500,
    COALESCE(p.min_stock, 5),
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM products p
CROSS JOIN warehouses w
JOIN stores s ON s.id = w.store_id
WHERE s.code IN ('LOJA-01', 'LOJA-02')
  AND w.code IN ('DEP-01', 'DEP-02')
  AND NOT EXISTS (
      SELECT 1 FROM inventory i WHERE i.product_id = p.id AND i.warehouse_id = w.id
  );

-- Preços na tabela PADRAO
INSERT INTO product_prices (
    id, price_table_id, product_id, price_type, unit_price, min_quantity,
    priority, status, active, created_at, updated_at, version
)
SELECT
    ('d9200000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 400), 12, '0'))::uuid,
    pt.id,
    p.id,
    'STANDARD',
    p.unit_price,
    1,
    100,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM price_tables pt
CROSS JOIN products p
WHERE pt.code = 'PADRAO'
  AND NOT EXISTS (
      SELECT 1 FROM product_prices pp
      WHERE pp.price_table_id = pt.id AND pp.product_id = p.id AND pp.price_type = 'STANDARD'
  );

INSERT INTO price_table_stores (price_table_id, store_id, created_at)
SELECT pt.id, s.id, NOW() AT TIME ZONE 'UTC'
FROM price_tables pt
CROSS JOIN stores s
WHERE pt.code = 'PADRAO'
  AND s.code IN ('LOJA-01', 'LOJA-02')
  AND NOT EXISTS (
      SELECT 1 FROM price_table_stores pts
      WHERE pts.price_table_id = pt.id AND pts.store_id = s.id
  );

-- ---------------------------------------------------------------------------
-- 5) Cliente consumidor final + vínculos por loja
-- ---------------------------------------------------------------------------
INSERT INTO customers (
    id, type, name, document, email, phone, status, active,
    organization_id, origin_store_id, created_at, updated_at, version
)
SELECT
    'e9000000-0000-4000-8000-000000000001',
    'PF',
    'Consumidor Final',
    '00000000000',
    'consumidor@systemcommerce.local',
    '11999990000',
    'ACTIVE',
    TRUE,
    o.id,
    s.id,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
JOIN stores s ON s.organization_id = o.id AND s.code = 'LOJA-01'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM customers c WHERE c.document = '00000000000');

INSERT INTO customer_store_relationships (
    id, customer_id, store_id, preferred_seller_profile_id, status,
    active, created_at, updated_at, version
)
SELECT
    ('e9100000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 500), 12, '0'))::uuid,
    c.id,
    s.id,
    sp.id,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM customers c
CROSS JOIN stores s
LEFT JOIN seller_profiles sp ON sp.seller_code = 'VEND-0001'
WHERE c.active = TRUE
  AND s.code IN ('LOJA-01', 'LOJA-02')
  AND NOT EXISTS (
      SELECT 1 FROM customer_store_relationships csr
      WHERE csr.customer_id = c.id AND csr.store_id = s.id
  );

-- ---------------------------------------------------------------------------
-- 6) Funcionários / vendedores extras
-- ---------------------------------------------------------------------------
INSERT INTO employees (
    id, organization_id, registration_number, name, job_title, status,
    user_id, can_sell, active, admission_date, created_at, updated_at, version
)
SELECT
    'b2000000-0000-4000-8000-000000000002',
    o.id,
    'EMP-0002',
    'Caixa Demo',
    'Operador de Caixa',
    'ACTIVE',
    u.id,
    TRUE,
    TRUE,
    CURRENT_DATE - 90,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
JOIN users u ON u.email = 'caixa@systemcommerce.local'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.registration_number = 'EMP-0002');

INSERT INTO employees (
    id, organization_id, registration_number, name, job_title, status,
    user_id, can_sell, active, admission_date, created_at, updated_at, version
)
SELECT
    'b2000000-0000-4000-8000-000000000003',
    o.id,
    'EMP-0003',
    'Vendedor Demo',
    'Vendedor',
    'ACTIVE',
    u.id,
    TRUE,
    TRUE,
    CURRENT_DATE - 60,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
JOIN users u ON u.email = 'vendedor@systemcommerce.local'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.registration_number = 'EMP-0003');

INSERT INTO employee_store_assignments (
    id, employee_id, store_id, assignment_type, start_date, primary_assignment, status,
    active, created_at, updated_at, version
)
SELECT
    ('b2100000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 600), 12, '0'))::uuid,
    e.id,
    s.id,
    'PERMANENT',
    CURRENT_DATE - 60,
    (s.code = 'LOJA-01'),
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM employees e
CROSS JOIN stores s
WHERE e.registration_number IN ('EMP-0002', 'EMP-0003')
  AND s.code IN ('LOJA-01', 'LOJA-02')
  AND NOT EXISTS (
      SELECT 1 FROM employee_store_assignments esa
      WHERE esa.employee_id = e.id AND esa.store_id = s.id
  );

INSERT INTO seller_profiles (
    id, organization_id, employee_id, seller_code, status, max_discount_percent,
    allows_external_sale, allows_other_stores, enabled_at, active,
    created_at, updated_at, version
)
SELECT
    'b3000000-0000-4000-8000-000000000002',
    e.organization_id,
    e.id,
    'VEND-0002',
    'ACTIVE',
    10.00,
    FALSE,
    TRUE,
    CURRENT_DATE - 60,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM employees e
WHERE e.registration_number = 'EMP-0003'
  AND NOT EXISTS (SELECT 1 FROM seller_profiles sp WHERE sp.seller_code = 'VEND-0002');

INSERT INTO seller_store_assignments (
    id, seller_profile_id, store_id, start_date, primary_assignment, temporary_assignment,
    allows_register_sale, status, active, created_at, updated_at, version
)
SELECT
    ('b3100000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 700), 12, '0'))::uuid,
    sp.id,
    s.id,
    CURRENT_DATE - 60,
    (s.code = 'LOJA-01'),
    FALSE,
    TRUE,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM seller_profiles sp
CROSS JOIN stores s
WHERE sp.seller_code IN ('VEND-0001', 'VEND-0002')
  AND s.code IN ('LOJA-01', 'LOJA-02')
  AND NOT EXISTS (
      SELECT 1 FROM seller_store_assignments ssa
      WHERE ssa.seller_profile_id = sp.id AND ssa.store_id = s.id
  );

-- ---------------------------------------------------------------------------
-- 7) Grupos, promoções, comissões, metas
-- ---------------------------------------------------------------------------
INSERT INTO store_groups (
    id, organization_id, code, name, description, status, active,
    created_at, updated_at, version
)
SELECT
    'd9300000-0000-4000-8000-000000000001',
    o.id,
    'GRP-SUDESTE',
    'Grupo Sudeste',
    'Lojas da região sudeste',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM store_groups g WHERE g.code = 'GRP-SUDESTE');

INSERT INTO store_group_members (
    id, store_group_id, store_id, active, created_at, updated_at, version
)
SELECT
    ('d9310000-0000-4000-8000-' || lpad(to_hex((row_number() OVER ())::int + 800), 12, '0'))::uuid,
    g.id,
    s.id,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM store_groups g
CROSS JOIN stores s
WHERE g.code = 'GRP-SUDESTE'
  AND s.code IN ('LOJA-01', 'LOJA-02')
  AND NOT EXISTS (
      SELECT 1 FROM store_group_members m
      WHERE m.store_group_id = g.id AND m.store_id = s.id
  );

INSERT INTO promotions (
    id, organization_id, code, name, description, channel, status, priority,
    valid_from, valid_to, active, created_at, updated_at, version
)
SELECT
    'd9400000-0000-4000-8000-000000000001',
    o.id,
    'PROMO-MOUSE',
    'Promoção Mouse',
    'Desconto demo no mouse',
    'POS',
    'ACTIVE',
    50,
    NOW() AT TIME ZONE 'UTC' - INTERVAL '7 days',
    NOW() AT TIME ZONE 'UTC' + INTERVAL '60 days',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM promotions p WHERE p.code = 'PROMO-MOUSE');

INSERT INTO promotion_stores (promotion_id, store_id)
SELECT pr.id, s.id
FROM promotions pr
CROSS JOIN stores s
WHERE pr.code = 'PROMO-MOUSE'
  AND s.code IN ('LOJA-01', 'LOJA-02')
  AND NOT EXISTS (
      SELECT 1 FROM promotion_stores ps
      WHERE ps.promotion_id = pr.id AND ps.store_id = s.id
  );

INSERT INTO promotion_products (
    id, promotion_id, product_id, promotional_price, min_quantity,
    active, created_at, updated_at, version
)
SELECT
    'd9410000-0000-4000-8000-000000000001',
    pr.id,
    p.id,
    39.90,
    1,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM promotions pr
JOIN products p ON p.sku = 'MS-001'
WHERE pr.code = 'PROMO-MOUSE'
  AND NOT EXISTS (
      SELECT 1 FROM promotion_products pp
      WHERE pp.promotion_id = pr.id AND pp.product_id = p.id
  );

INSERT INTO commission_policies (
    id, organization_id, code, name, policy_version, channel, percent, fixed_amount,
    requires_paid, applies_on_confirmed, status, active, created_at, updated_at, version
)
SELECT
    'd9500000-0000-4000-8000-000000000001',
    o.id,
    'COM-PADRAO',
    'Comissão padrão 3%',
    1,
    'ANY',
    3.00,
    0,
    TRUE,
    FALSE,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM commission_policies cp WHERE cp.code = 'COM-PADRAO');

INSERT INTO sales_targets (
    id, organization_id, seller_profile_id, store_id, period_start, period_end,
    target_amount, target_quantity, status, active, created_at, updated_at, version
)
SELECT
    'd9510000-0000-4000-8000-000000000001',
    o.id,
    sp.id,
    s.id,
    date_trunc('month', CURRENT_DATE)::date,
    (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month - 1 day')::date,
    50000.00,
    100.000,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
JOIN seller_profiles sp ON sp.seller_code = 'VEND-0001'
JOIN stores s ON s.code = 'LOJA-01'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM sales_targets st WHERE st.id = 'd9510000-0000-4000-8000-000000000001');

-- ---------------------------------------------------------------------------
-- 8) Entrada de estoque + transferência (histórico)
-- ---------------------------------------------------------------------------
INSERT INTO stock_entries (
    id, organization_id, store_id, warehouse_id, number, supplier_name,
    document_number, entry_date, status, responsible_user_id, notes,
    confirmed_at, active, created_at, updated_at, version
)
SELECT
    'd9600000-0000-4000-8000-000000000001',
    o.id,
    s.id,
    w.id,
    'ENT-DEMO-001',
    'Fornecedor Demo Ltda',
    'NF-1001',
    CURRENT_DATE - 10,
    'CONFIRMED',
    u.id,
    'Entrada demo',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '10 days',
    TRUE,
    NOW() AT TIME ZONE 'UTC' - INTERVAL '10 days',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '10 days',
    0
FROM organizations o
JOIN stores s ON s.code = 'LOJA-01'
JOIN warehouses w ON w.store_id = s.id AND w.code = 'DEP-01'
JOIN users u ON u.email = 'estoque@systemcommerce.local'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM stock_entries se WHERE se.number = 'ENT-DEMO-001');

INSERT INTO stock_entry_items (
    id, entry_id, product_id, quantity, unit_cost, line_total,
    active, created_at, updated_at, version
)
SELECT
    'd9610000-0000-4000-8000-000000000001',
    se.id,
    p.id,
    10.000,
    p.cost_price,
    ROUND(10.000 * COALESCE(p.cost_price, 0), 2),
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stock_entries se
JOIN products p ON p.sku = 'TEC-001'
WHERE se.number = 'ENT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM stock_entry_items i WHERE i.entry_id = se.id AND i.product_id = p.id);

INSERT INTO stock_transfers (
    id, organization_id, number, origin_store_id, origin_warehouse_id,
    destination_store_id, destination_warehouse_id, requester_id, approver_id,
    dispatcher_id, receiver_id, requested_at, dispatched_at, received_at,
    status, observation, active, created_at, updated_at, version
)
SELECT
    'd9700000-0000-4000-8000-000000000001',
    o.id,
    'TRF-DEMO-001',
    s1.id, w1.id,
    s2.id, w2.id,
    u.id, u.id, u.id, u.id,
    NOW() AT TIME ZONE 'UTC' - INTERVAL '5 days',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '4 days',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '3 days',
    'RECEIVED',
    'Transferência demo LOJA-01 → LOJA-02',
    TRUE,
    NOW() AT TIME ZONE 'UTC' - INTERVAL '5 days',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '3 days',
    0
FROM organizations o
JOIN stores s1 ON s1.code = 'LOJA-01'
JOIN warehouses w1 ON w1.store_id = s1.id AND w1.code = 'DEP-01'
JOIN stores s2 ON s2.code = 'LOJA-02'
JOIN warehouses w2 ON w2.store_id = s2.id AND w2.code = 'DEP-02'
JOIN users u ON u.email = 'estoque@systemcommerce.local'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM stock_transfers t WHERE t.number = 'TRF-DEMO-001');

INSERT INTO stock_transfer_items (
    id, transfer_id, product_id, quantity_requested, quantity_approved,
    quantity_dispatched, quantity_received, quantity_divergent,
    active, created_at, updated_at, version
)
SELECT
    'd9710000-0000-4000-8000-000000000001',
    t.id,
    p.id,
    5.000, 5.000, 5.000, 5.000, 0,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stock_transfers t
JOIN products p ON p.sku = 'CAB-USB'
WHERE t.number = 'TRF-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM stock_transfer_items i WHERE i.transfer_id = t.id AND i.product_id = p.id);

INSERT INTO stock_transfer_status_history (
    id, transfer_id, from_status, to_status, changed_by, reason, created_at
)
SELECT
    'd9720000-0000-4000-8000-000000000001',
    t.id,
    NULL,
    'RECEIVED',
    u.id,
    'Carga demo',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '3 days'
FROM stock_transfers t
JOIN users u ON u.email = 'estoque@systemcommerce.local'
WHERE t.number = 'TRF-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM stock_transfer_status_history h WHERE h.transfer_id = t.id);

-- ---------------------------------------------------------------------------
-- 9) Sessão de caixa FECHADA + venda PAID histórica (LOJA-01)
-- ---------------------------------------------------------------------------
INSERT INTO cash_sessions (
    id, store_id, terminal_id, operator_id, opened_at, closed_at,
    opening_amount, status, expected_amount, counted_amount, difference_amount,
    opening_notes, closing_notes, open_idempotency_key, close_idempotency_key,
    active, created_at, updated_at, version
)
SELECT
    'd9800000-0000-4000-8000-000000000001',
    s.id,
    t.id,
    u.id,
    NOW() AT TIME ZONE 'UTC' - INTERVAL '2 days',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '1 day',
    100.00,
    'CLOSED',
    149.90,
    149.90,
    0,
    'Abertura demo',
    'Fechamento demo',
    'demo-open-001',
    'demo-close-001',
    TRUE,
    NOW() AT TIME ZONE 'UTC' - INTERVAL '2 days',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '1 day',
    0
FROM stores s
JOIN pos_terminals t ON t.store_id = s.id AND t.code = 'TERM-01'
JOIN users u ON u.email = 'caixa@systemcommerce.local'
WHERE s.code = 'LOJA-01'
  AND NOT EXISTS (SELECT 1 FROM cash_sessions cs WHERE cs.open_idempotency_key = 'demo-open-001');

INSERT INTO cash_movements (
    id, cash_session_id, type, amount, reason, notes, occurred_at,
    executed_by_id, active, created_at, updated_at, version, cash_effect
)
SELECT
    'd9810000-0000-4000-8000-000000000001',
    cs.id,
    'OPENING',
    100.00,
    'Fundo de troco',
    'Demo',
    cs.opened_at,
    cs.operator_id,
    TRUE,
    cs.opened_at,
    cs.opened_at,
    0,
    'INCREASE'
FROM cash_sessions cs
WHERE cs.open_idempotency_key = 'demo-open-001'
  AND NOT EXISTS (
      SELECT 1 FROM cash_movements m
      WHERE m.cash_session_id = cs.id AND m.type = 'OPENING'
  );

-- Venda PAID (mouse)
INSERT INTO sales (
    id, customer_id, seller_id, seller_profile_id, status, sale_number, sale_date,
    channel, store_id, terminal_id, cash_session_id, warehouse_id, organization_id,
    price_table_id, subtotal, discount_amount, surcharge_amount, freight_amount,
    total_amount, seller_code_snapshot, seller_name_snapshot, notes, active,
    created_at, updated_at, version, idempotency_key
)
SELECT
    'd9820000-0000-4000-8000-000000000001',
    c.id,
    u.id,
    sp.id,
    'PAID',
    'DEMO-0001',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '2 days' + INTERVAL '2 hours',
    'POS',
    s.id,
    t.id,
    cs.id,
    w.id,
    o.id,
    pt.id,
    49.90,
    0,
    0,
    0,
    49.90,
    sp.seller_code,
    'Vendedor Padrão',
    'Venda demo histórica',
    TRUE,
    NOW() AT TIME ZONE 'UTC' - INTERVAL '2 days' + INTERVAL '2 hours',
    NOW() AT TIME ZONE 'UTC' - INTERVAL '2 days' + INTERVAL '2 hours',
    0,
    'demo-sale-001'
FROM cash_sessions cs
JOIN stores s ON s.id = cs.store_id
JOIN pos_terminals t ON t.id = cs.terminal_id
JOIN warehouses w ON w.id = t.warehouse_id
JOIN organizations o ON o.id = s.organization_id
JOIN users u ON u.email = 'caixa@systemcommerce.local'
JOIN customers c ON c.document = '00000000000'
JOIN seller_profiles sp ON sp.seller_code = 'VEND-0001'
JOIN price_tables pt ON pt.code = 'PADRAO'
WHERE cs.open_idempotency_key = 'demo-open-001'
  AND NOT EXISTS (SELECT 1 FROM sales sl WHERE sl.sale_number = 'DEMO-0001');

INSERT INTO sale_items (
    id, sale_id, product_id, quantity, unit_price, discount_amount,
    line_subtotal, line_total, description, price_source, active,
    created_at, updated_at, version
)
SELECT
    'd9830000-0000-4000-8000-000000000001',
    sl.id,
    p.id,
    1.000,
    49.90,
    0,
    49.90,
    49.90,
    p.name,
    'CATALOG',
    TRUE,
    sl.created_at,
    sl.updated_at,
    0
FROM sales sl
JOIN products p ON p.sku = 'MS-001'
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (SELECT 1 FROM sale_items si WHERE si.sale_id = sl.id AND si.product_id = p.id);

INSERT INTO sale_status_history (id, sale_id, from_status, to_status, reason, changed_at, changed_by)
SELECT
    'd9840000-0000-4000-8000-000000000001',
    sl.id,
    NULL,
    'PAID',
    'Carga demo',
    sl.sale_date,
    sl.seller_id
FROM sales sl
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (SELECT 1 FROM sale_status_history h WHERE h.sale_id = sl.id);

INSERT INTO payments (
    id, sale_id, method, amount, status, paid_at, installments,
    informed_amount, applied_amount, change_amount, tendered_amount,
    cash_session_id, responsible_user_id, store_id, idempotency_key,
    active, created_at, updated_at, version
)
SELECT
    'd9850000-0000-4000-8000-000000000001',
    sl.id,
    'CASH',
    49.90,
    'CONFIRMED',
    sl.sale_date,
    1,
    50.00,
    49.90,
    0.10,
    50.00,
    sl.cash_session_id,
    sl.seller_id,
    sl.store_id,
    'demo-pay-001',
    TRUE,
    sl.sale_date,
    sl.sale_date,
    0
FROM sales sl
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.idempotency_key = 'demo-pay-001');

INSERT INTO cash_movements (
    id, cash_session_id, type, amount, reason, notes, occurred_at,
    executed_by_id, sale_id, active, created_at, updated_at, version, cash_effect
)
SELECT
    'd9810000-0000-4000-8000-000000000002',
    sl.cash_session_id,
    'CASH_SALE',
    49.90,
    'Venda DEMO-0001',
    'Demo',
    sl.sale_date,
    sl.seller_id,
    sl.id,
    TRUE,
    sl.sale_date,
    sl.sale_date,
    0,
    'INCREASE'
FROM sales sl
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (
      SELECT 1 FROM cash_movements m WHERE m.sale_id = sl.id AND m.type = 'CASH_SALE'
  );

-- Baixa de estoque da venda demo (ajuste + movimento SALE)
UPDATE inventory i
SET quantity = GREATEST(i.quantity - 1, 0),
    updated_at = NOW() AT TIME ZONE 'UTC'
FROM sales sl
JOIN sale_items si ON si.sale_id = sl.id
JOIN products p ON p.id = si.product_id AND p.sku = 'MS-001'
WHERE sl.sale_number = 'DEMO-0001'
  AND i.product_id = p.id
  AND i.warehouse_id = sl.warehouse_id
  AND NOT EXISTS (
      SELECT 1 FROM stock_movements sm
      WHERE sm.reference_type = 'SALE' AND sm.reference_id = sl.id
  );

INSERT INTO stock_movements (
    id, product_id, type, quantity, previous_quantity, new_quantity,
    reference_type, reference_id, reason, user_id, warehouse_id, store_id, created_at
)
SELECT
    'f9000000-0000-4000-8000-000000000901',
    si.product_id,
    'SALE',
    si.quantity,
    i.quantity + si.quantity,
    i.quantity,
    'SALE',
    sl.id,
    'Venda demo DEMO-0001',
    sl.seller_id,
    sl.warehouse_id,
    sl.store_id,
    sl.sale_date
FROM sales sl
JOIN sale_items si ON si.sale_id = sl.id
JOIN inventory i ON i.product_id = si.product_id AND i.warehouse_id = sl.warehouse_id
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (
      SELECT 1 FROM stock_movements sm
      WHERE sm.reference_type = 'SALE' AND sm.reference_id = sl.id
  );

INSERT INTO receipt_print_logs (
    id, print_type, sequence_no, sale_id, payment_id, cash_session_id,
    requested_by_id, copies, layout, is_reprint, authentication_id,
    terminal_id, active, created_at, updated_at, version
)
SELECT
    'd9860000-0000-4000-8000-000000000001',
    'SALE',
    1,
    sl.id,
    p.id,
    sl.cash_session_id,
    sl.seller_id,
    1,
    'THERMAL_80',
    FALSE,
    'AUTH-DEMO-0001',
    sl.terminal_id,
    TRUE,
    sl.sale_date,
    sl.sale_date,
    0
FROM sales sl
JOIN payments p ON p.sale_id = sl.id
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (SELECT 1 FROM receipt_print_logs r WHERE r.authentication_id = 'AUTH-DEMO-0001');

INSERT INTO sale_seller_history (
    id, sale_id, previous_seller_profile_id, new_seller_profile_id,
    previous_seller_code, new_seller_code, previous_seller_name, new_seller_name,
    changed_by, reason, created_at, created_by
)
SELECT
    'd9870000-0000-4000-8000-000000000001',
    sl.id,
    NULL,
    sl.seller_profile_id,
    NULL,
    sl.seller_code_snapshot,
    NULL,
    sl.seller_name_snapshot,
    sl.seller_id,
    'Atribuição inicial demo',
    sl.sale_date,
    sl.seller_id
FROM sales sl
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (SELECT 1 FROM sale_seller_history h WHERE h.sale_id = sl.id);

INSERT INTO store_sale_sequences (store_id, last_value, prefix, updated_at)
SELECT s.id, 1, 'DEMO-', NOW() AT TIME ZONE 'UTC'
FROM stores s
WHERE s.code = 'LOJA-01'
ON CONFLICT (store_id) DO UPDATE
SET last_value = GREATEST(store_sale_sequences.last_value, EXCLUDED.last_value),
    updated_at = NOW() AT TIME ZONE 'UTC';

-- ---------------------------------------------------------------------------
-- 10) Auditoria amostra
-- ---------------------------------------------------------------------------
INSERT INTO audit_logs (
    id, entity_name, entity_id, action, details, performed_by, performed_at,
    module, outcome, store_id, organization_id
)
SELECT
    'd9900000-0000-4000-8000-000000000001',
    'Sale',
    sl.id,
    'CREATE',
    'Carga demo — venda DEMO-0001',
    sl.seller_id,
    sl.sale_date,
    'SALE',
    'SUCCESS',
    sl.store_id,
    sl.organization_id
FROM sales sl
WHERE sl.sale_number = 'DEMO-0001'
  AND NOT EXISTS (SELECT 1 FROM audit_logs a WHERE a.id = 'd9900000-0000-4000-8000-000000000001');

COMMIT;

-- Resumo
SELECT 'users' AS tabela, COUNT(*) AS total FROM users
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'customers', COUNT(*) FROM customers
UNION ALL SELECT 'inventory', COUNT(*) FROM inventory
UNION ALL SELECT 'store_products', COUNT(*) FROM store_products
UNION ALL SELECT 'cash_sessions', COUNT(*) FROM cash_sessions
UNION ALL SELECT 'sales', COUNT(*) FROM sales
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'promotions', COUNT(*) FROM promotions
UNION ALL SELECT 'stock_transfers', COUNT(*) FROM stock_transfers
UNION ALL SELECT 'stock_entries', COUNT(*) FROM stock_entries
ORDER BY 1;
