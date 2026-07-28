-- V220: Permissões Prompts 80–86
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000240', 'INTEGRATION_READ', 'Consultar integrações', 'INTEGRATION',
     'Consultar canais, contas e jobs', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000241', 'INTEGRATION_MANAGE', 'Gerenciar integrações', 'INTEGRATION',
     'CRUD canais/contas e disparar sync', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000242', 'PUBLIC_API_READ', 'Consultar API pública', 'INTEGRATION',
     'Consultar credenciais e logs da API pública', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000243', 'PUBLIC_API_MANAGE', 'Gerenciar API pública', 'INTEGRATION',
     'Criar/revogar credenciais da API pública', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000244', 'WEBHOOK_READ', 'Consultar webhooks', 'INTEGRATION',
     'Consultar subscriptions e entregas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000245', 'WEBHOOK_MANAGE', 'Gerenciar webhooks', 'INTEGRATION',
     'CRUD subscriptions e replay', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000246', 'MOBILE_DEVICE_MANAGE', 'Gerenciar dispositivos mobile', 'MOBILE',
     'Registrar tokens push do app', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'INTEGRATION_READ', 'INTEGRATION_MANAGE',
      'PUBLIC_API_READ', 'PUBLIC_API_MANAGE',
      'WEBHOOK_READ', 'WEBHOOK_MANAGE',
      'MOBILE_DEVICE_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
