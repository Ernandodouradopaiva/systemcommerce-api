-- V249: Permissões prompts 114–118
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000321', 'CASH_FLOW_READ', 'Consultar fluxo de caixa', 'FINANCE',
     'Consultar fluxo realizado e projetado', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000322', 'CASH_FLOW_EXPORT', 'Exportar fluxo de caixa', 'FINANCE',
     'Exportar fluxo de caixa', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000323', 'FINANCIAL_PERIOD_READ', 'Consultar períodos financeiros', 'FINANCE',
     'Consultar períodos e fechamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000324', 'FINANCIAL_PERIOD_CLOSE', 'Fechar período financeiro', 'FINANCE',
     'Executar fechamento financeiro', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000325', 'FINANCIAL_PERIOD_REOPEN', 'Reabrir período financeiro', 'FINANCE',
     'Reabrir período fechado com motivo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000326', 'INCOME_STATEMENT_READ', 'Consultar DRE gerencial', 'FINANCE',
     'Consultar demonstração de resultado gerencial', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000327', 'INCOME_STATEMENT_MANAGE', 'Gerir layout DRE gerencial', 'FINANCE',
     'Gerir layouts, linhas e mapeamentos da DRE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000328', 'INCOME_STATEMENT_EXPORT', 'Exportar DRE gerencial', 'FINANCE',
     'Exportar DRE gerencial', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000329', 'FINANCE_DASHBOARD_READ', 'Dashboard financeiro', 'FINANCE',
     'Consultar dashboard financeiro', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000330', 'FINANCE_REPORT_READ', 'Relatórios financeiros', 'FINANCE',
     'Consultar relatórios financeiros', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000331', 'FINANCE_REPORT_EXPORT', 'Exportar relatórios financeiros', 'FINANCE',
     'Exportar relatórios financeiros (CSV/PDF)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'CASH_FLOW_READ', 'CASH_FLOW_EXPORT',
      'FINANCIAL_PERIOD_READ', 'FINANCIAL_PERIOD_CLOSE', 'FINANCIAL_PERIOD_REOPEN',
      'INCOME_STATEMENT_READ', 'INCOME_STATEMENT_MANAGE', 'INCOME_STATEMENT_EXPORT',
      'FINANCE_DASHBOARD_READ', 'FINANCE_REPORT_READ', 'FINANCE_REPORT_EXPORT'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
