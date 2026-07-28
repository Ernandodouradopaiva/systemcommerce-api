-- V233: Permissões AP/AR (Prompts 96–101)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000276', 'PAYABLE_READ', 'Consultar contas a pagar', 'FINANCE',
     'Consultar contas a pagar e parcelas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000277', 'PAYABLE_CREATE', 'Criar contas a pagar', 'FINANCE',
     'Gerar contas a pagar a partir de documentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000278', 'PAYABLE_UPDATE', 'Atualizar contas a pagar', 'FINANCE',
     'Atualizar rascunhos de contas a pagar', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000279', 'PAYABLE_CANCEL', 'Cancelar contas a pagar', 'FINANCE',
     'Cancelar contas a pagar com motivo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000280', 'PAYABLE_MANUAL_CREATE', 'Lançamento manual a pagar', 'FINANCE',
     'Criar despesa/conta a pagar manual', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000281', 'PAYABLE_RENEGOTIATE', 'Renegociar contas a pagar', 'FINANCE',
     'Renegociar parcelas a pagar', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000282', 'PAYABLE_SETTLE', 'Liquidar contas a pagar', 'FINANCE',
     'Confirmar pagamentos de contas a pagar', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000283', 'RECEIVABLE_READ', 'Consultar contas a receber', 'FINANCE',
     'Consultar contas a receber e parcelas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000284', 'RECEIVABLE_CREATE', 'Criar contas a receber', 'FINANCE',
     'Gerar contas a receber a partir de documentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000285', 'RECEIVABLE_UPDATE', 'Atualizar contas a receber', 'FINANCE',
     'Atualizar rascunhos de contas a receber', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000286', 'RECEIVABLE_CANCEL', 'Cancelar contas a receber', 'FINANCE',
     'Cancelar contas a receber com motivo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000287', 'RECEIVABLE_MANUAL_CREATE', 'Cobrança manual a receber', 'FINANCE',
     'Criar cobrança/conta a receber manual', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000288', 'RECEIVABLE_RENEGOTIATE', 'Renegociar contas a receber', 'FINANCE',
     'Renegociar parcelas a receber', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000289', 'RECEIVABLE_WRITE_OFF', 'Baixa sem recebimento', 'FINANCE',
     'Write-off de contas a receber', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000290', 'RECEIVABLE_SETTLE', 'Receber contas a receber', 'FINANCE',
     'Confirmar recebimentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'PAYABLE_READ', 'PAYABLE_CREATE', 'PAYABLE_UPDATE', 'PAYABLE_CANCEL',
      'PAYABLE_MANUAL_CREATE', 'PAYABLE_RENEGOTIATE', 'PAYABLE_SETTLE',
      'RECEIVABLE_READ', 'RECEIVABLE_CREATE', 'RECEIVABLE_UPDATE', 'RECEIVABLE_CANCEL',
      'RECEIVABLE_MANUAL_CREATE', 'RECEIVABLE_RENEGOTIATE', 'RECEIVABLE_WRITE_OFF', 'RECEIVABLE_SETTLE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO finance_generation_settings (
    id, organization_id, generate_payable_on_receipt, generate_receivable_on_invoice,
    generate_and_settle_pos_cash, active, created_at, updated_at, version
)
SELECT
    'c2000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000001',
    TRUE, TRUE, TRUE, TRUE,
    NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
WHERE EXISTS (SELECT 1 FROM organizations WHERE id = 'b1000000-0000-4000-8000-000000000001')
ON CONFLICT (organization_id) DO NOTHING;
