-- V106: permissão INVENTORY_ADJUST (compatível com Prompt 5) e perfis adicionais
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES (
    'a1000000-0000-4000-8000-00000000001a',
    'INVENTORY_ADJUST',
    'Ajustar estoque',
    'INVENTORY',
    'Ajustes manuais de estoque',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

-- Garante vínculo da nova permissão ao ADMIN
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code = 'INVENTORY_ADJUST'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Gerente
INSERT INTO roles (id, code, name, description, active, created_at, updated_at, version)
VALUES (
    'b1000000-0000-4000-8000-000000000002',
    'MANAGER',
    'Gerente',
    'Gestão operacional e relatórios',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 'b1000000-0000-4000-8000-000000000002'::uuid, p.id, NOW() AT TIME ZONE 'UTC'
FROM permissions p
WHERE p.code IN (
    'USER_READ',
    'CUSTOMER_READ', 'CUSTOMER_CREATE', 'CUSTOMER_UPDATE', 'CUSTOMER_DELETE',
    'CATEGORY_READ', 'CATEGORY_MANAGE',
    'PRODUCT_READ', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'PRODUCT_DELETE',
    'INVENTORY_READ', 'INVENTORY_MOVE', 'INVENTORY_ADJUST',
    'SALE_READ', 'SALE_CREATE', 'SALE_CONFIRM', 'SALE_CANCEL',
    'PAYMENT_MANAGE', 'DASHBOARD_READ', 'REPORT_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 'b1000000-0000-4000-8000-000000000002'::uuid
      AND rp.permission_id = p.id
);

-- Vendedor
INSERT INTO roles (id, code, name, description, active, created_at, updated_at, version)
VALUES (
    'b1000000-0000-4000-8000-000000000003',
    'SELLER',
    'Vendedor',
    'Operações de venda e consulta de cadastros',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 'b1000000-0000-4000-8000-000000000003'::uuid, p.id, NOW() AT TIME ZONE 'UTC'
FROM permissions p
WHERE p.code IN (
    'CUSTOMER_READ', 'CUSTOMER_CREATE', 'CUSTOMER_UPDATE',
    'PRODUCT_READ', 'CATEGORY_READ',
    'INVENTORY_READ',
    'SALE_READ', 'SALE_CREATE', 'SALE_CONFIRM',
    'PAYMENT_MANAGE', 'DASHBOARD_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 'b1000000-0000-4000-8000-000000000003'::uuid
      AND rp.permission_id = p.id
);

-- Estoquista
INSERT INTO roles (id, code, name, description, active, created_at, updated_at, version)
VALUES (
    'b1000000-0000-4000-8000-000000000004',
    'STOCK_KEEPER',
    'Estoquista',
    'Consulta e movimentação de estoque',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 'b1000000-0000-4000-8000-000000000004'::uuid, p.id, NOW() AT TIME ZONE 'UTC'
FROM permissions p
WHERE p.code IN (
    'PRODUCT_READ', 'CATEGORY_READ',
    'INVENTORY_READ', 'INVENTORY_MOVE', 'INVENTORY_ADJUST'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 'b1000000-0000-4000-8000-000000000004'::uuid
      AND rp.permission_id = p.id
);
