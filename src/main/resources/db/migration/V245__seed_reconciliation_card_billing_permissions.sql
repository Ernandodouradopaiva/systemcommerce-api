-- V245: Permissões prompts 111–113
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000310', 'BANK_RECONCILIATION_READ', 'Consultar conciliação bancária', 'FINANCE',
     'Consultar extratos e conciliações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000311', 'BANK_RECONCILIATION_IMPORT', 'Importar extratos bancários', 'FINANCE',
     'Importar OFX/CSV e lançar extrato manual', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000312', 'BANK_RECONCILIATION_MATCH', 'Conciliar extratos', 'FINANCE',
     'Sugerir, confirmar e desfazer conciliações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000313', 'BANK_RECONCILIATION_CREATE_MISSING', 'Criar lançamento ausente', 'FINANCE',
     'Criar movimento financeiro a partir do extrato', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000314', 'CARD_ACQUIRER_READ', 'Consultar adquirentes/cartões', 'FINANCE',
     'Consultar adquirentes, taxas e recebíveis de cartão', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000315', 'CARD_ACQUIRER_MANAGE', 'Gerir adquirentes/cartões', 'FINANCE',
     'Cadastrar adquirentes, bandeiras e planos de taxa', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000316', 'CARD_SETTLEMENT_MANAGE', 'Liquidar cartões', 'FINANCE',
     'Liquidar e conciliar recebíveis de cartão', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000317', 'BILLING_READ', 'Consultar cobranças', 'FINANCE',
     'Consultar boletos e PIX', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000318', 'BILLING_CREATE', 'Gerar cobranças', 'FINANCE',
     'Gerar boletos e cobranças PIX', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000319', 'BILLING_CANCEL', 'Cancelar cobranças', 'FINANCE',
     'Cancelar cobranças preservando histórico', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000320', 'BILLING_WEBHOOK', 'Receber webhooks de cobrança', 'FINANCE',
     'Processar webhooks bancários de cobrança', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'BANK_RECONCILIATION_READ', 'BANK_RECONCILIATION_IMPORT', 'BANK_RECONCILIATION_MATCH',
      'BANK_RECONCILIATION_CREATE_MISSING',
      'CARD_ACQUIRER_READ', 'CARD_ACQUIRER_MANAGE', 'CARD_SETTLEMENT_MANAGE',
      'BILLING_READ', 'BILLING_CREATE', 'BILLING_CANCEL', 'BILLING_WEBHOOK'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
