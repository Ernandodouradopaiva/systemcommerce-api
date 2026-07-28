-- V177: permissões Fornecedores / Orçamentos / Pedidos de Venda (Prompts 56–58)

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000141', 'SUPPLIER_READ', 'Consultar fornecedores', 'SUPPLIER',
     'Listar e consultar fornecedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000142', 'SUPPLIER_CREATE', 'Criar fornecedores', 'SUPPLIER',
     'Cadastrar fornecedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000143', 'SUPPLIER_UPDATE', 'Atualizar fornecedores', 'SUPPLIER',
     'Editar, ativar e inativar fornecedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000144', 'SUPPLIER_DELETE', 'Excluir fornecedores', 'SUPPLIER',
     'Excluir fornecedores (lógica se houver movimentação)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000145', 'QUOTE_READ', 'Consultar orçamentos', 'QUOTE',
     'Listar e consultar orçamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000146', 'QUOTE_CREATE', 'Criar orçamentos', 'QUOTE',
     'Criar e duplicar orçamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000147', 'QUOTE_UPDATE', 'Atualizar orçamentos', 'QUOTE',
     'Editar orçamentos e alterar status operacional', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000148', 'QUOTE_CANCEL', 'Cancelar orçamentos', 'QUOTE',
     'Cancelar orçamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000149', 'QUOTE_CONVERT', 'Converter orçamentos', 'QUOTE',
     'Converter orçamento em pedido de venda', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000150', 'SALES_ORDER_READ', 'Consultar pedidos de venda', 'SALES_ORDER',
     'Listar e consultar pedidos de venda', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000151', 'SALES_ORDER_CREATE', 'Criar pedidos de venda', 'SALES_ORDER',
     'Criar pedidos de venda', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000152', 'SALES_ORDER_UPDATE', 'Atualizar pedidos de venda', 'SALES_ORDER',
     'Editar e avançar fluxo do pedido', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000153', 'SALES_ORDER_CANCEL', 'Cancelar pedidos de venda', 'SALES_ORDER',
     'Cancelar pedidos de venda', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000154', 'SALES_ORDER_GENERATE_SALE', 'Gerar venda a partir do pedido', 'SALES_ORDER',
     'Gerar venda (Sale) a partir de pedido faturável', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'SUPPLIER_READ', 'SUPPLIER_CREATE', 'SUPPLIER_UPDATE', 'SUPPLIER_DELETE',
      'QUOTE_READ', 'QUOTE_CREATE', 'QUOTE_UPDATE', 'QUOTE_CANCEL', 'QUOTE_CONVERT',
      'SALES_ORDER_READ', 'SALES_ORDER_CREATE', 'SALES_ORDER_UPDATE', 'SALES_ORDER_CANCEL', 'SALES_ORDER_GENERATE_SALE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CASHIER'
  AND p.code IN ('QUOTE_READ', 'SALES_ORDER_READ', 'SUPPLIER_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
