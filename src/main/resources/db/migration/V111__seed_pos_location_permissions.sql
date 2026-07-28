-- V111: permissões de loja, depósito e terminal PDV

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
(
    'a1000000-0000-4000-8000-00000000001c',
    'STORE_READ',
    'Consultar lojas',
    'STORE',
    'Listar e consultar lojas/estabelecimentos',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-00000000001d',
    'STORE_MANAGE',
    'Gerenciar lojas',
    'STORE',
    'Criar, editar, ativar e inativar lojas',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-00000000001e',
    'WAREHOUSE_READ',
    'Consultar depósitos',
    'WAREHOUSE',
    'Listar e consultar depósitos/locais de estoque',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-00000000001f',
    'WAREHOUSE_MANAGE',
    'Gerenciar depósitos',
    'WAREHOUSE',
    'Criar, editar, ativar e inativar depósitos',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000020',
    'POS_TERMINAL_READ',
    'Consultar terminais PDV',
    'POS',
    'Listar e consultar terminais de PDV',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000021',
    'POS_TERMINAL_MANAGE',
    'Gerenciar terminais PDV',
    'POS',
    'Criar, editar, ativar, inativar e vincular depósito de terminais',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

-- ADMIN recebe todas as novas
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'STORE_READ', 'STORE_MANAGE',
      'WAREHOUSE_READ', 'WAREHOUSE_MANAGE',
      'POS_TERMINAL_READ', 'POS_TERMINAL_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- MANAGER: leitura + gestão operacional de localização
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MANAGER'
  AND p.code IN (
      'STORE_READ', 'STORE_MANAGE',
      'WAREHOUSE_READ', 'WAREHOUSE_MANAGE',
      'POS_TERMINAL_READ', 'POS_TERMINAL_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
