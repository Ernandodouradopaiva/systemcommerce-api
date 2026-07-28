-- V182: permissões compras/recebimento + aliases SALESPERSON (Prompts 60–63)
-- seller_profiles já cobre o cadastro de vendedores; SALESPERSON_* espelha SELLER_* para o contrato do Prompt 63.

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000155', 'PURCHASE_ORDER_READ', 'Consultar pedidos de compra', 'PURCHASE',
     'Listar e consultar pedidos de compra', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000156', 'PURCHASE_ORDER_CREATE', 'Criar pedidos de compra', 'PURCHASE',
     'Criar pedidos de compra', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000157', 'PURCHASE_ORDER_UPDATE', 'Atualizar pedidos de compra', 'PURCHASE',
     'Editar e avançar status do pedido de compra', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000158', 'PURCHASE_ORDER_CANCEL', 'Cancelar pedidos de compra', 'PURCHASE',
     'Cancelar pedidos de compra', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000159', 'PURCHASE_RECEIPT_READ', 'Consultar recebimentos', 'PURCHASE',
     'Consultar recebimentos de mercadoria', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000160', 'PURCHASE_RECEIPT_CREATE', 'Registrar recebimentos', 'PURCHASE',
     'Registrar recebimento físico (parcial/total)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000161', 'SALES_ORDER_BILL', 'Faturar pedido de venda', 'SALES_ORDER',
     'Faturamento que efetiva venda, estoque e histórico', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000162', 'SALESPERSON_READ', 'Consultar vendedores (alias)', 'SALESPERSON',
     'Alias Prompt 63 — equivalente a SELLER_READ', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000163', 'SALESPERSON_CREATE', 'Criar vendedores (alias)', 'SALESPERSON',
     'Alias Prompt 63 — equivalente a SELLER_CREATE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000164', 'SALESPERSON_UPDATE', 'Atualizar vendedores (alias)', 'SALESPERSON',
     'Alias Prompt 63 — equivalente a SELLER_UPDATE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000165', 'SALESPERSON_DELETE', 'Excluir/desativar vendedores (alias)', 'SALESPERSON',
     'Alias Prompt 63 — desativar perfil de vendedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'PURCHASE_ORDER_READ', 'PURCHASE_ORDER_CREATE', 'PURCHASE_ORDER_UPDATE', 'PURCHASE_ORDER_CANCEL',
      'PURCHASE_RECEIPT_READ', 'PURCHASE_RECEIPT_CREATE',
      'SALES_ORDER_BILL',
      'SALESPERSON_READ', 'SALESPERSON_CREATE', 'SALESPERSON_UPDATE', 'SALESPERSON_DELETE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Espelha SALESPERSON_* para quem já tem SELLER_* (idempotente)
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT rp.role_id, sp.id, NOW() AT TIME ZONE 'UTC'
FROM role_permissions rp
JOIN permissions seller_p ON seller_p.id = rp.permission_id AND seller_p.code = 'SELLER_READ'
JOIN permissions sp ON sp.code = 'SALESPERSON_READ'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = sp.id
);

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT rp.role_id, sp.id, NOW() AT TIME ZONE 'UTC'
FROM role_permissions rp
JOIN permissions seller_p ON seller_p.id = rp.permission_id AND seller_p.code = 'SELLER_CREATE'
JOIN permissions sp ON sp.code = 'SALESPERSON_CREATE'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = sp.id
);

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT rp.role_id, sp.id, NOW() AT TIME ZONE 'UTC'
FROM role_permissions rp
JOIN permissions seller_p ON seller_p.id = rp.permission_id AND seller_p.code = 'SELLER_UPDATE'
JOIN permissions sp ON sp.code = 'SALESPERSON_UPDATE'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = sp.id
);

-- default_commission_percent no seller_profiles se ainda não existir
ALTER TABLE seller_profiles
    ADD COLUMN IF NOT EXISTS default_commission_percent NUMERIC(7, 4) NOT NULL DEFAULT 0;

COMMENT ON COLUMN seller_profiles.default_commission_percent IS 'Comissão padrão % (Prompt 63)';
