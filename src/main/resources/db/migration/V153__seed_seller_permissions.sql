-- V153: permissões de vendedores
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000080', 'SELLER_READ', 'Consultar vendedores', 'SELLER',
     'Listar e consultar vendedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000081', 'SELLER_CREATE', 'Habilitar vendedor', 'SELLER',
     'Habilitar profissional como vendedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000082', 'SELLER_UPDATE', 'Atualizar vendedor', 'SELLER',
     'Atualizar e desabilitar vendedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000083', 'SELLER_ASSIGN_STORE', 'Autorizar vendedor em loja', 'SELLER',
     'Conceder/remover autorização comercial por loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000084', 'SELLER_VIEW_PERFORMANCE', 'Desempenho do vendedor', 'SELLER',
     'Consultar vendas e metas do vendedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000085', 'SELLER_AUTHORIZE_OTHER_STORE', 'Autorizar outra loja', 'SELLER',
     'Autorizar vendedor em loja fora da lotação/base', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'SELLER_READ', 'SELLER_CREATE', 'SELLER_UPDATE',
      'SELLER_ASSIGN_STORE', 'SELLER_VIEW_PERFORMANCE', 'SELLER_AUTHORIZE_OTHER_STORE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
