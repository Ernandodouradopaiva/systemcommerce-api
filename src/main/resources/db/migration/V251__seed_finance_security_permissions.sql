-- V251: Permissões granulares de segurança financeira (Prompt 119)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000332', 'FINANCE_DISCOUNT_GRANT', 'Conceder desconto financeiro', 'FINANCE',
     'Conceder desconto em liquidações/parcelas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000333', 'FINANCE_PAYMENT_APPROVE', 'Aprovar pagamento financeiro', 'FINANCE',
     'Aprovar operações financeiras em duas etapas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000334', 'BANK_ACCOUNT_SENSITIVE_READ', 'Ver dados bancários sensíveis', 'FINANCE',
     'Visualizar dados bancários mascarados/sensíveis (não segredos)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000335', 'FINANCE_CONSOLIDATED_READ', 'Consultar visão consolidada', 'FINANCE',
     'Consultar saldos e indicadores consolidados entre lojas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000336', 'FINANCE_APPROVAL_REQUEST', 'Solicitar aprovação financeira', 'FINANCE',
     'Abrir solicitação de aprovação em duas etapas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000337', 'FINANCE_APPROVAL_DECIDE', 'Decidir aprovação financeira', 'FINANCE',
     'Aprovar ou rejeitar solicitações financeiras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000338', 'FINANCE_MIGRATION_RUN', 'Executar migração financeira', 'FINANCE',
     'Executar backfill/migração de dados financeiros legados', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000339', 'FINANCE_AUDIT_READ', 'Consultar auditoria financeira', 'FINANCE',
     'Consultar eventos de auditoria do módulo financeiro', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000340', 'FINANCE_BALANCE_ACCESS', 'Acessar saldos financeiros', 'FINANCE',
     'Acesso explícito a consulta de saldo (auditado)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'FINANCE_DISCOUNT_GRANT', 'FINANCE_PAYMENT_APPROVE', 'BANK_ACCOUNT_SENSITIVE_READ',
      'FINANCE_CONSOLIDATED_READ', 'FINANCE_APPROVAL_REQUEST', 'FINANCE_APPROVAL_DECIDE',
      'FINANCE_MIGRATION_RUN', 'FINANCE_AUDIT_READ', 'FINANCE_BALANCE_ACCESS'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
