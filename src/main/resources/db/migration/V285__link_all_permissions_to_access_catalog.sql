-- V285: Expõe todas as permissões ativas no catálogo admin (módulo/recurso)
-- Cria recursos faltantes e vincula permissions.resource_id / module_id / action_id

-- Recursos base por módulo (idempotente)
INSERT INTO system_resources (id, module_id, code, name, description, admin_route, sort_order, active, created_at, updated_at, version)
SELECT gen_random_uuid(), m.id, v.code, v.name, v.description, v.admin_route, v.sort_order, TRUE,
       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM system_modules m
JOIN (VALUES
    ('ACCESS', 'USERS', 'Usuários', 'Cadastro de usuários', '/administracao/usuarios', 10),
    ('ACCESS', 'ROLES', 'Perfis legados', 'Perfis/roles legados', '/access-groups', 15),
    ('ACCESS', 'ACCESS_GROUPS', 'Grupos de usuários', 'Grupos e permissões', '/administracao/grupos', 20),
    ('ACCESS', 'PERMISSIONS', 'Permissões', 'Catálogo de permissões', '/administracao/catalogo-permissoes', 30),
    ('ACCESS', 'HIERARCHY', 'Hierarquia', 'Equipes e hierarquia', NULL, 40),
    ('ACCESS', 'SESSIONS', 'Sessões', 'Sessões e tokens', NULL, 50),
    ('CADASTROS', 'CUSTOMERS', 'Clientes', 'Cadastro de clientes', '/customers', 10),
    ('CADASTROS', 'SUPPLIERS', 'Fornecedores', 'Cadastro de fornecedores', '/suppliers', 20),
    ('CADASTROS', 'EMPLOYEES', 'Profissionais', 'Cadastro de profissionais', '/employees', 30),
    ('CADASTROS', 'SELLERS', 'Vendedores', 'Cadastro de vendedores', '/sellers', 40),
    ('PRODUCTS', 'PRODUCTS', 'Produtos', 'Catálogo de produtos', '/products', 10),
    ('PRODUCTS', 'CATEGORIES', 'Categorias', 'Categorias de produtos', NULL, 20),
    ('PRODUCTS', 'BRANDS', 'Marcas', 'Marcas', NULL, 30),
    ('PRODUCTS', 'MANUFACTURERS', 'Fabricantes', 'Fabricantes', NULL, 40),
    ('PRODUCTS', 'PRODUCT_LINES', 'Linhas', 'Linhas de produto', NULL, 50),
    ('PURCHASES', 'PURCHASE_REQUESTS', 'Solicitações de compra', 'Solicitações', NULL, 10),
    ('PURCHASES', 'PURCHASE_ORDERS', 'Pedidos de compra', 'Pedidos de compra', NULL, 20),
    ('PURCHASES', 'PURCHASE_RECEIPTS', 'Recebimentos', 'Recebimentos de compra', NULL, 30),
    ('PURCHASES', 'PURCHASE_QUOTATIONS', 'Cotações', 'Cotações de compra', NULL, 40),
    ('PURCHASES', 'SUPPLIER_RETURNS', 'Devoluções a fornecedor', 'Devoluções', NULL, 50),
    ('INVENTORY', 'INVENTORY', 'Estoque', 'Saldos e movimentos', '/inventory', 10),
    ('INVENTORY', 'STOCK_TRANSFERS', 'Transferências', 'Transferências entre lojas', NULL, 20),
    ('SALES', 'SALES', 'Vendas', 'Vendas administrativas', '/sales', 10),
    ('SALES', 'SALES_ORDERS', 'Pedidos de venda', 'Pedidos de venda', NULL, 20),
    ('SALES', 'QUOTES', 'Orçamentos', 'Orçamentos', NULL, 30),
    ('POS', 'POS', 'PDV', 'Ponto de venda', '/pos', 10),
    ('POS', 'CASH', 'Caixa', 'Sessões de caixa', NULL, 20),
    ('FINANCE', 'FINANCE', 'Financeiro', 'Operações financeiras', NULL, 10),
    ('FINANCE', 'PAYABLES', 'Contas a pagar', 'Contas a pagar', NULL, 20),
    ('FINANCE', 'RECEIVABLES', 'Contas a receber', 'Contas a receber', NULL, 30),
    ('FINANCE', 'RECONCILIATION', 'Conciliação', 'Conciliação', NULL, 40),
    ('FISCAL', 'FISCAL', 'Fiscal', 'Documentos fiscais', NULL, 10),
    ('REPORTS', 'REPORTS', 'Relatórios', 'Relatórios', '/reports', 10),
    ('REPORTS', 'DASHBOARD', 'Dashboards', 'Painéis', '/dashboard', 20),
    ('AUDIT', 'AUDIT', 'Auditoria', 'Trilhas de auditoria', NULL, 10),
    ('INTEGRATIONS', 'INTEGRATIONS', 'Integrações', 'Integrações externas', NULL, 10),
    ('ADMIN', 'ORGANIZATION', 'Organização', 'Organização e lojas', NULL, 10),
    ('ADMIN', 'STORES', 'Lojas', 'Lojas', NULL, 20),
    ('ADMIN', 'GENERAL', 'Geral', 'Administração geral', NULL, 90)
) AS v(module_code, code, name, description, admin_route, sort_order)
  ON m.code = v.module_code
WHERE NOT EXISTS (
    SELECT 1 FROM system_resources r WHERE r.module_id = m.id AND r.code = v.code
);

-- module_id a partir do campo legado / prefixo
UPDATE permissions p
SET module_id = m.id
FROM system_modules m
WHERE p.module_id IS NULL
  AND UPPER(p.module) = m.code;

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'ACCESS')
WHERE p.module_id IS NULL AND (p.code LIKE 'USER_%' OR p.code LIKE 'ROLE_%' OR p.code LIKE 'ACCESS_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'CADASTROS')
WHERE p.module_id IS NULL AND (p.code LIKE 'CUSTOMER_%' OR p.code LIKE 'SUPPLIER_%'
   OR p.code LIKE 'EMPLOYEE_%' OR p.code LIKE 'SELLER_%' OR p.code LIKE 'SALESPERSON_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'PRODUCTS')
WHERE p.module_id IS NULL AND (p.code LIKE 'PRODUCT_%' OR p.code LIKE 'CATEGORY_%'
   OR p.code LIKE 'BRAND_%' OR p.code LIKE 'MANUFACTURER_%' OR p.code LIKE 'PRODUCT_LINE_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'PURCHASES')
WHERE p.module_id IS NULL AND (p.code LIKE 'PURCHASE_%' OR p.code LIKE 'SUPPLIER_RETURN_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'INVENTORY')
WHERE p.module_id IS NULL AND (p.code LIKE 'INVENTORY_%' OR p.code LIKE 'STOCK_%' OR p.code LIKE 'TRANSFER_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'SALES')
WHERE p.module_id IS NULL AND (p.code LIKE 'SALE_%' OR p.code LIKE 'SALES_%' OR p.code LIKE 'QUOTE_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'POS')
WHERE p.module_id IS NULL AND (p.code LIKE 'POS_%' OR p.code LIKE 'CASH_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'FINANCE')
WHERE p.module_id IS NULL AND (p.code LIKE 'FINANCE_%' OR p.code LIKE 'PAYABLE_%'
   OR p.code LIKE 'RECEIVABLE_%' OR p.code LIKE 'PAYMENT_%' OR p.code LIKE 'RECONCILIATION_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'FISCAL')
WHERE p.module_id IS NULL AND (p.code LIKE 'FISCAL_%' OR p.code LIKE 'NFE_%' OR p.code LIKE 'NFCE_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'REPORTS')
WHERE p.module_id IS NULL AND (p.code LIKE 'REPORT_%' OR p.code LIKE 'DASHBOARD_%'
   OR p.code LIKE 'EXECUTIVE_%' OR p.code LIKE 'ANALYTICS_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'AUDIT')
WHERE p.module_id IS NULL AND (p.code LIKE 'AUDIT_%' OR p.code LIKE 'ACCESS_AUDIT_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'INTEGRATIONS')
WHERE p.module_id IS NULL AND (p.code LIKE 'INTEGRATION_%' OR p.code LIKE 'WEBHOOK_%' OR p.code LIKE 'MARKETPLACE_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'ADMIN')
WHERE p.module_id IS NULL AND (p.code LIKE 'ORGANIZATION_%' OR p.code LIKE 'STORE_%' OR p.code LIKE 'ADMIN_%');

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'ADMIN')
WHERE p.module_id IS NULL;

-- resource_id por prefixo de código
UPDATE permissions p
SET resource_id = r.id
FROM system_resources r
WHERE p.resource_id IS NULL
  AND p.module_id = r.module_id
  AND (
        (p.code LIKE 'USER_%' AND r.code = 'USERS')
     OR (p.code LIKE 'ROLE_%' AND r.code = 'ROLES')
     OR (p.code LIKE 'ACCESS_GROUP_%' AND r.code = 'ACCESS_GROUPS')
     OR (p.code LIKE 'ACCESS_CATALOG_%' AND r.code = 'PERMISSIONS')
     OR (p.code LIKE 'HIERARCHY_%' AND r.code = 'HIERARCHY')
     OR (p.code LIKE 'SESSION_%' AND r.code = 'SESSIONS')
     OR (p.code LIKE 'CUSTOMER_%' AND r.code = 'CUSTOMERS')
     OR (p.code LIKE 'SUPPLIER_RETURN_%' AND r.code = 'SUPPLIER_RETURNS')
     OR (p.code LIKE 'SUPPLIER_%' AND r.code = 'SUPPLIERS')
     OR (p.code LIKE 'EMPLOYEE_%' AND r.code = 'EMPLOYEES')
     OR (p.code LIKE 'SELLER_%' AND r.code = 'SELLERS')
     OR (p.code LIKE 'SALESPERSON_%' AND r.code = 'SELLERS')
     OR (p.code LIKE 'PRODUCT_LINE_%' AND r.code = 'PRODUCT_LINES')
     OR (p.code LIKE 'PRODUCT_%' AND r.code = 'PRODUCTS')
     OR (p.code LIKE 'CATEGORY_%' AND r.code = 'CATEGORIES')
     OR (p.code LIKE 'BRAND_%' AND r.code = 'BRANDS')
     OR (p.code LIKE 'MANUFACTURER_%' AND r.code = 'MANUFACTURERS')
     OR (p.code LIKE 'PURCHASE_REQUEST_%' AND r.code = 'PURCHASE_REQUESTS')
     OR (p.code LIKE 'PURCHASE_ORDER_%' AND r.code = 'PURCHASE_ORDERS')
     OR (p.code LIKE 'PURCHASE_RECEIPT_%' AND r.code = 'PURCHASE_RECEIPTS')
     OR (p.code LIKE 'PURCHASE_QUOTATION_%' AND r.code = 'PURCHASE_QUOTATIONS')
     OR (p.code LIKE 'PURCHASE_%' AND r.code = 'PURCHASE_ORDERS')
     OR (p.code LIKE 'INVENTORY_%' AND r.code = 'INVENTORY')
     OR (p.code LIKE 'STOCK_%' AND r.code = 'INVENTORY')
     OR (p.code LIKE 'TRANSFER_%' AND r.code = 'STOCK_TRANSFERS')
     OR (p.code LIKE 'SALES_ORDER_%' AND r.code = 'SALES_ORDERS')
     OR (p.code LIKE 'SALE_%' AND r.code = 'SALES')
     OR (p.code LIKE 'QUOTE_%' AND r.code = 'QUOTES')
     OR (p.code LIKE 'POS_%' AND r.code = 'POS')
     OR (p.code LIKE 'CASH_%' AND r.code = 'CASH')
     OR (p.code LIKE 'PAYABLE_%' AND r.code = 'PAYABLES')
     OR (p.code LIKE 'RECEIVABLE_%' AND r.code = 'RECEIVABLES')
     OR (p.code LIKE 'RECONCILIATION_%' AND r.code = 'RECONCILIATION')
     OR (p.code LIKE 'PAYMENT_%' AND r.code = 'FINANCE')
     OR (p.code LIKE 'FINANCE_%' AND r.code = 'FINANCE')
     OR (p.code LIKE 'FISCAL_%' AND r.code = 'FISCAL')
     OR (p.code LIKE 'NFE_%' AND r.code = 'FISCAL')
     OR (p.code LIKE 'NFCE_%' AND r.code = 'FISCAL')
     OR (p.code LIKE 'REPORT_%' AND r.code = 'REPORTS')
     OR (p.code LIKE 'DASHBOARD_%' AND r.code = 'DASHBOARD')
     OR (p.code LIKE 'EXECUTIVE_%' AND r.code = 'DASHBOARD')
     OR (p.code LIKE 'ANALYTICS_%' AND r.code = 'REPORTS')
     OR (p.code LIKE 'AUDIT_%' AND r.code = 'AUDIT')
     OR (p.code LIKE 'ACCESS_AUDIT_%' AND r.code = 'AUDIT')
     OR (p.code LIKE 'INTEGRATION_%' AND r.code = 'INTEGRATIONS')
     OR (p.code LIKE 'WEBHOOK_%' AND r.code = 'INTEGRATIONS')
     OR (p.code LIKE 'MARKETPLACE_%' AND r.code = 'INTEGRATIONS')
     OR (p.code LIKE 'ORGANIZATION_%' AND r.code = 'ORGANIZATION')
     OR (p.code LIKE 'STORE_%' AND r.code = 'STORES')
     OR (p.code LIKE 'PRIVILEGED_%' AND r.code = 'ACCESS_GROUPS')
     OR (p.code LIKE 'ACCESS_REVIEW_%' AND r.code = 'ACCESS_GROUPS')
     OR (p.code LIKE 'ACCESS_REPORT_%' AND r.code = 'PERMISSIONS')
     OR (p.code LIKE 'EFFECTIVE_PERMISSION_%' AND r.code = 'PERMISSIONS')
  );

-- Fallback: recurso GENERAL do módulo
UPDATE permissions p
SET resource_id = r.id
FROM system_resources r
WHERE p.resource_id IS NULL
  AND p.module_id = r.module_id
  AND r.code = 'GENERAL';

-- Cria GENERAL por módulo se ainda houver órfãos
INSERT INTO system_resources (id, module_id, code, name, description, admin_route, sort_order, active, created_at, updated_at, version)
SELECT gen_random_uuid(), m.id, 'GENERAL', 'Geral', 'Demais permissões do módulo', NULL, 999, TRUE,
       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM system_modules m
WHERE EXISTS (
    SELECT 1 FROM permissions p WHERE p.module_id = m.id AND p.resource_id IS NULL AND p.active = TRUE
)
AND NOT EXISTS (
    SELECT 1 FROM system_resources r WHERE r.module_id = m.id AND r.code = 'GENERAL'
);

UPDATE permissions p
SET resource_id = r.id
FROM system_resources r
WHERE p.resource_id IS NULL
  AND p.module_id = r.module_id
  AND r.code = 'GENERAL';

-- action_id pelo sufixo
UPDATE permissions p
SET action_id = a.id
FROM system_actions a
WHERE p.action_id IS NULL
  AND UPPER(regexp_replace(p.code, '^.*_', '')) = a.code;
