-- V159: permissões StoreProduct

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000100', 'STORE_PRODUCT_READ', 'Consultar produto por loja', 'STORE_PRODUCT',
     'Consultar disponibilidade e configuração comercial por loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000101', 'STORE_PRODUCT_MANAGE', 'Gerenciar produto por loja', 'STORE_PRODUCT',
     'Habilitar, bloquear e editar configuração produto×loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000102', 'STORE_PRODUCT_BULK_ASSIGN', 'Atribuição em massa produto×loja', 'STORE_PRODUCT',
     'Habilitar/copiar configuração em várias lojas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('STORE_PRODUCT_READ', 'STORE_PRODUCT_MANAGE', 'STORE_PRODUCT_BULK_ASSIGN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CASHIER'
  AND p.code = 'STORE_PRODUCT_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
