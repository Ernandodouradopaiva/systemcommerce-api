-- V142: permissões de configurações do PDV

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000050', 'POS_SETTINGS_READ', 'Consultar configurações do PDV', 'POS',
     'Consultar definições, valores e configuração efetiva do PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000051', 'POS_SETTINGS_MANAGE', 'Administrar configurações do PDV', 'POS',
     'Criar, alterar e remover configurações do PDV (global/loja/terminal)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('POS_SETTINGS_READ', 'POS_SETTINGS_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Operadores podem apenas ler a efetiva no PDV
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SELLER'
  AND p.code = 'POS_SETTINGS_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
