-- V223: Permissões Prompts 87–89
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000250', 'EXECUTIVE_DASHBOARD_READ', 'Dashboard executivo', 'DASHBOARD',
     'Consultar dashboard executivo e drill-down', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000251', 'EXECUTIVE_DASHBOARD_EXPORT', 'Exportar dashboard executivo', 'DASHBOARD',
     'Exportar indicadores do dashboard executivo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000252', 'BI_ANALYTICS_READ', 'Consultar camada analítica', 'BI',
     'Consultar views/materialized views BI', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000253', 'BI_ANALYTICS_MANAGE', 'Gerenciar refresh BI', 'BI',
     'Disparar refresh da camada analítica', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000254', 'PURCHASE_SUGGESTION_READ', 'Consultar sugestões de compra', 'PURCHASE',
     'Consultar sugestões e execuções', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000255', 'PURCHASE_SUGGESTION_MANAGE', 'Gerenciar sugestões de compra', 'PURCHASE',
     'Executar simulações e converter sugestões', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'EXECUTIVE_DASHBOARD_READ', 'EXECUTIVE_DASHBOARD_EXPORT',
      'BI_ANALYTICS_READ', 'BI_ANALYTICS_MANAGE',
      'PURCHASE_SUGGESTION_READ', 'PURCHASE_SUGGESTION_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
