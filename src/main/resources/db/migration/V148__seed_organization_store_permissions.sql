-- V148: permissões de organização e loja (granulares)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000060', 'ORGANIZATION_READ', 'Consultar organização', 'ORGANIZATION',
     'Consulta dados da organização', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000061', 'ORGANIZATION_MANAGE', 'Gerenciar organização', 'ORGANIZATION',
     'Cadastrar e atualizar organização', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000062', 'STORE_CREATE', 'Cadastrar loja', 'STORE',
     'Criar novas lojas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000063', 'STORE_UPDATE', 'Atualizar loja', 'STORE',
     'Editar dados da loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000064', 'STORE_ACTIVATE', 'Ativar loja', 'STORE',
     'Ativar loja inativa', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000065', 'STORE_DEACTIVATE', 'Inativar loja', 'STORE',
     'Inativar loja com validações de segurança', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'ORGANIZATION_READ', 'ORGANIZATION_MANAGE',
      'STORE_CREATE', 'STORE_UPDATE', 'STORE_ACTIVATE', 'STORE_DEACTIVATE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ADMIN já tem STORE_READ/STORE_MANAGE (V111); reforça vínculo se faltar
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('STORE_READ', 'STORE_MANAGE', 'ORGANIZATION_READ', 'ORGANIZATION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
