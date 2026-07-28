-- V185: permissões de status e dados bancários de fornecedores (Prompt 57)

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000166', 'SUPPLIER_STATUS_MANAGE', 'Gerenciar status de fornecedores', 'SUPPLIER',
     'Ativar, inativar, bloquear e desbloquear fornecedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000167', 'SUPPLIER_BANK_DATA_READ', 'Consultar dados bancários de fornecedores', 'SUPPLIER',
     'Consultar contas bancárias/PIX cadastradas do fornecedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000168', 'SUPPLIER_BANK_DATA_MANAGE', 'Gerenciar dados bancários de fornecedores', 'SUPPLIER',
     'Criar, editar e remover contas bancárias/PIX do fornecedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('SUPPLIER_STATUS_MANAGE', 'SUPPLIER_BANK_DATA_READ', 'SUPPLIER_BANK_DATA_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Espelha SUPPLIER_STATUS_MANAGE para quem já possui SUPPLIER_UPDATE (idempotente)
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT rp.role_id, sp.id, NOW() AT TIME ZONE 'UTC'
FROM role_permissions rp
JOIN permissions update_p ON update_p.id = rp.permission_id AND update_p.code = 'SUPPLIER_UPDATE'
JOIN permissions sp ON sp.code = 'SUPPLIER_STATUS_MANAGE'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = sp.id
);
