-- V155: permissões de acesso por loja e contexto
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000090', 'USER_STORE_ACCESS_READ', 'Consultar acesso por loja', 'STORE_ACCESS',
     'Consultar acessos usuário↔loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000091', 'USER_STORE_ACCESS_MANAGE', 'Gerenciar acesso por loja', 'STORE_ACCESS',
     'Conceder/revogar acessos e loja padrão', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000092', 'STORE_CONTEXT_SWITCH', 'Trocar contexto de loja', 'STORE_ACCESS',
     'Selecionar loja ativa na sessão', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000093', 'GLOBAL_STORE_ACCESS', 'Acesso global a lojas', 'STORE_ACCESS',
     'Acessar todas as lojas da organização sem vínculo explícito', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000094', 'STORE_CONSOLIDATED_READ', 'Relatórios consolidados', 'STORE_ACCESS',
     'Consultar dados consolidados multi-loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'USER_STORE_ACCESS_READ', 'USER_STORE_ACCESS_MANAGE',
      'STORE_CONTEXT_SWITCH', 'GLOBAL_STORE_ACCESS', 'STORE_CONSOLIDATED_READ'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MANAGER'
  AND p.code IN (
      'USER_STORE_ACCESS_READ', 'USER_STORE_ACCESS_MANAGE',
      'STORE_CONTEXT_SWITCH', 'STORE_CONSOLIDATED_READ'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
