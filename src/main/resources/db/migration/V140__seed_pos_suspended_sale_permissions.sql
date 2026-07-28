-- V140: permissões de vendas suspensas / recuperação

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000046', 'POS_SUSPENDED_SALE_READ', 'Consultar vendas suspensas', 'POS',
     'Listar e pesquisar vendas suspensas do PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000047', 'POS_SUSPENDED_SALE_RESUME', 'Recuperar venda suspensa própria', 'POS',
     'Recuperar venda suspensa do próprio operador', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000048', 'POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR', 'Recuperar/assumir venda de outro operador', 'POS',
     'Recuperar, assumir ou liberar bloqueio de venda suspensa de outro operador', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000049', 'POS_SUSPENDED_SALE_DISCARD', 'Descartar venda suspensa', 'POS',
     'Descartar venda suspensa com auditoria', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER', 'SELLER')
  AND p.code IN ('POS_SUSPENDED_SALE_READ', 'POS_SUSPENDED_SALE_RESUME')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR', 'POS_SUSPENDED_SALE_DISCARD')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
