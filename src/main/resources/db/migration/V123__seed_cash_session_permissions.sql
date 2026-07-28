-- V123: permissões de sessão de caixa

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
(
    'a1000000-0000-4000-8000-000000000022',
    'POS_OPEN_CASH',
    'Abrir caixa',
    'POS',
    'Abrir sessão de caixa no PDV',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000023',
    'POS_CLOSE_CASH',
    'Fechar caixa',
    'POS',
    'Iniciar e concluir fechamento da própria sessão de caixa',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000024',
    'POS_VIEW_SESSION',
    'Consultar sessão de caixa',
    'POS',
    'Consultar sessões, resumo e conferência',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000025',
    'POS_FORCE_CLOSE_CASH',
    'Forçar fechamento de caixa',
    'POS',
    'Fechar sessão de outro operador ou forçar encerramento',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('POS_OPEN_CASH', 'POS_CLOSE_CASH', 'POS_VIEW_SESSION', 'POS_FORCE_CLOSE_CASH')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MANAGER'
  AND p.code IN ('POS_OPEN_CASH', 'POS_CLOSE_CASH', 'POS_VIEW_SESSION', 'POS_FORCE_CLOSE_CASH')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- SELLER: operar caixa básico (sem force close)
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SELLER'
  AND p.code IN ('POS_OPEN_CASH', 'POS_CLOSE_CASH', 'POS_VIEW_SESSION')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
