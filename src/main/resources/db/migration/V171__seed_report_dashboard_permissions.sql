-- V171: permissões multiloja para relatórios e dashboard ERP

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000136', 'REPORT_STORE_READ', 'Relatórios por loja', 'REPORT',
     'Consultar relatórios filtrados por loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000137', 'REPORT_MULTI_STORE_READ', 'Relatórios multi-loja', 'REPORT',
     'Consultar relatórios consolidados das lojas acessíveis', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000138', 'REPORT_GLOBAL_READ', 'Relatórios globais', 'REPORT',
     'Consultar relatórios de todas as lojas da organização', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000139', 'DASHBOARD_STORE_READ', 'Dashboard por loja', 'DASHBOARD',
     'Consultar indicadores filtrados por loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000140', 'DASHBOARD_GLOBAL_READ', 'Dashboard global', 'DASHBOARD',
     'Consultar indicadores consolidados de todas as lojas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'REPORT_STORE_READ', 'REPORT_MULTI_STORE_READ', 'REPORT_GLOBAL_READ',
      'DASHBOARD_STORE_READ', 'DASHBOARD_GLOBAL_READ'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
