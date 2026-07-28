-- V241: Permissões prompts 105–110
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000291', 'ADVANCE_READ', 'Consultar adiantamentos', 'FINANCE',
     'Consultar adiantamentos de clientes e fornecedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000292', 'ADVANCE_CREATE', 'Criar adiantamentos', 'FINANCE',
     'Registrar adiantamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000293', 'ADVANCE_APPLY', 'Aplicar adiantamentos', 'FINANCE',
     'Aplicar saldo de adiantamento a documentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000294', 'ADVANCE_REFUND', 'Estornar/reembolsar adiantamentos', 'FINANCE',
     'Reembolsar ou cancelar adiantamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000295', 'FINANCIAL_POLICY_READ', 'Consultar políticas financeiras', 'FINANCE',
     'Consultar juros, multas e descontos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000296', 'FINANCIAL_POLICY_MANAGE', 'Gerir políticas financeiras', 'FINANCE',
     'Criar e alterar políticas de encargos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000297', 'FINANCIAL_TRANSFER_READ', 'Consultar transferências', 'FINANCE',
     'Consultar transferências entre contas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000298', 'FINANCIAL_TRANSFER_CREATE', 'Criar transferências', 'FINANCE',
     'Criar transferências entre contas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000299', 'FINANCIAL_TRANSFER_CONFIRM', 'Confirmar transferências', 'FINANCE',
     'Confirmar ou estornar transferências', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000300', 'FINANCIAL_ENTRY_READ', 'Consultar lançamentos manuais', 'FINANCE',
     'Consultar lançamentos financeiros manuais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000301', 'FINANCIAL_ENTRY_CREATE', 'Criar lançamentos manuais', 'FINANCE',
     'Criar lançamentos financeiros manuais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000302', 'FINANCIAL_ENTRY_CONFIRM', 'Confirmar lançamentos manuais', 'FINANCE',
     'Confirmar lançamentos manuais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000303', 'FINANCIAL_ENTRY_CANCEL', 'Cancelar lançamentos manuais', 'FINANCE',
     'Cancelar rascunhos de lançamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000304', 'FINANCIAL_ENTRY_REVERSE', 'Estornar lançamentos manuais', 'FINANCE',
     'Estornar lançamentos confirmados', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000305', 'FINANCIAL_REVERSAL_READ', 'Consultar estornos', 'FINANCE',
     'Consultar reversões financeiras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000306', 'FINANCIAL_REVERSAL_CREATE', 'Criar estornos', 'FINANCE',
     'Criar e confirmar reversões financeiras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000307', 'FINANCIAL_RENEGOTIATION_READ', 'Consultar renegociações', 'FINANCE',
     'Consultar renegociações financeiras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000308', 'FINANCIAL_RENEGOTIATION_CREATE', 'Criar renegociações', 'FINANCE',
     'Criar e confirmar renegociações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000309', 'FINANCIAL_RENEGOTIATION_CANCEL', 'Cancelar renegociações', 'FINANCE',
     'Cancelar renegociações quando possível', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'ADVANCE_READ', 'ADVANCE_CREATE', 'ADVANCE_APPLY', 'ADVANCE_REFUND',
      'FINANCIAL_POLICY_READ', 'FINANCIAL_POLICY_MANAGE',
      'FINANCIAL_TRANSFER_READ', 'FINANCIAL_TRANSFER_CREATE', 'FINANCIAL_TRANSFER_CONFIRM',
      'FINANCIAL_ENTRY_READ', 'FINANCIAL_ENTRY_CREATE', 'FINANCIAL_ENTRY_CONFIRM',
      'FINANCIAL_ENTRY_CANCEL', 'FINANCIAL_ENTRY_REVERSE',
      'FINANCIAL_REVERSAL_READ', 'FINANCIAL_REVERSAL_CREATE',
      'FINANCIAL_RENEGOTIATION_READ', 'FINANCIAL_RENEGOTIATION_CREATE', 'FINANCIAL_RENEGOTIATION_CANCEL'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
