-- V162: permissões de transferência de estoque + pricing multiloja

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000110', 'STOCK_TRANSFER_READ', 'Consultar transferências', 'STOCK_TRANSFER',
     'Consultar transferências de estoque', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000111', 'STOCK_TRANSFER_CREATE', 'Criar transferências', 'STOCK_TRANSFER',
     'Criar e solicitar transferências', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000112', 'STOCK_TRANSFER_APPROVE', 'Aprovar transferências', 'STOCK_TRANSFER',
     'Aprovar ou rejeitar transferências', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000113', 'STOCK_TRANSFER_DISPATCH', 'Expedir transferências', 'STOCK_TRANSFER',
     'Preparar e despachar transferências', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000114', 'STOCK_TRANSFER_RECEIVE', 'Receber transferências', 'STOCK_TRANSFER',
     'Receber total/parcial e registrar divergência', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000115', 'STOCK_TRANSFER_CANCEL', 'Cancelar transferências', 'STOCK_TRANSFER',
     'Cancelar transferências', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000116', 'STORE_GROUP_MANAGE', 'Gerenciar grupos de lojas', 'PRICING',
     'Cadastrar grupos e vincular lojas para precificação', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000117', 'PROMOTION_MANAGE', 'Gerenciar promoções', 'PRICING',
     'Cadastrar e manter promoções por loja/canal', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'STOCK_TRANSFER_READ', 'STOCK_TRANSFER_CREATE', 'STOCK_TRANSFER_APPROVE',
      'STOCK_TRANSFER_DISPATCH', 'STOCK_TRANSFER_RECEIVE', 'STOCK_TRANSFER_CANCEL',
      'STORE_GROUP_MANAGE', 'PROMOTION_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MANAGER'
  AND p.code IN (
      'STOCK_TRANSFER_READ', 'STOCK_TRANSFER_CREATE', 'STOCK_TRANSFER_APPROVE',
      'STOCK_TRANSFER_DISPATCH', 'STOCK_TRANSFER_RECEIVE', 'STOCK_TRANSFER_CANCEL',
      'STORE_GROUP_MANAGE', 'PROMOTION_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
