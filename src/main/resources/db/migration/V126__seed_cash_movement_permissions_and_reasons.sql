-- V126: permissões de movimentação de caixa + seeds de motivos

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
(
    'a1000000-0000-4000-8000-000000000026',
    'POS_CASH_SUPPLY',
    'Registrar suprimento de caixa',
    'POS',
    'Registrar suprimento (entrada de numerário) na sessão aberta',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000027',
    'POS_CASH_WITHDRAWAL',
    'Registrar sangria de caixa',
    'POS',
    'Registrar sangria (saída de numerário) na sessão aberta',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000028',
    'POS_CASH_MOVEMENT_READ',
    'Consultar movimentações de caixa',
    'POS',
    'Listar movimentações, saldo físico e resumo por tipo',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-000000000029',
    'POS_CASH_MOVEMENT_REVERSE',
    'Estornar movimentação de caixa',
    'POS',
    'Estornar movimentação mediante lançamento inverso',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
),
(
    'a1000000-0000-4000-8000-00000000002a',
    'POS_AUTHORIZE_HIGH_WITHDRAWAL',
    'Autorizar sangria elevada / saldo insuficiente',
    'POS',
    'Autorizar sangria acima do limite ou que ultrapasse o saldo físico',
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
  AND p.code IN (
      'POS_CASH_SUPPLY', 'POS_CASH_WITHDRAWAL', 'POS_CASH_MOVEMENT_READ',
      'POS_CASH_MOVEMENT_REVERSE', 'POS_AUTHORIZE_HIGH_WITHDRAWAL'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MANAGER'
  AND p.code IN (
      'POS_CASH_SUPPLY', 'POS_CASH_WITHDRAWAL', 'POS_CASH_MOVEMENT_READ',
      'POS_CASH_MOVEMENT_REVERSE', 'POS_AUTHORIZE_HIGH_WITHDRAWAL'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SELLER'
  AND p.code IN ('POS_CASH_SUPPLY', 'POS_CASH_WITHDRAWAL', 'POS_CASH_MOVEMENT_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO cash_movement_reasons (id, code, description, applies_to, active, created_at, updated_at, version)
VALUES
('c2000000-0000-4000-8000-000000000001', 'SUPPLY_OPENING_CHANGE', 'Troco / reforço de fundo', 'SUPPLY', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('c2000000-0000-4000-8000-000000000002', 'SUPPLY_BANK', 'Recebimento do banco / cofre', 'SUPPLY', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('c2000000-0000-4000-8000-000000000003', 'SUPPLY_OTHER', 'Outros suprimentos', 'SUPPLY', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('c2000000-0000-4000-8000-000000000004', 'WITHDRAW_SAFE', 'Depósito no cofre', 'WITHDRAWAL', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('c2000000-0000-4000-8000-000000000005', 'WITHDRAW_BANK', 'Depósito bancário', 'WITHDRAWAL', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('c2000000-0000-4000-8000-000000000006', 'WITHDRAW_EXPENSE', 'Despesa / pagamento avulso', 'WITHDRAWAL', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('c2000000-0000-4000-8000-000000000007', 'WITHDRAW_OTHER', 'Outras sangrias', 'WITHDRAWAL', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('c2000000-0000-4000-8000-000000000008', 'BOTH_CORRECTION', 'Correção operacional', 'BOTH', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;
