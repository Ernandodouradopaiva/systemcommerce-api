-- V196: Permissões cadeia de compras Prompts 59–63
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000170', 'PURCHASE_REQUEST_READ', 'Consultar solicitações de compra', 'PURCHASE',
     'Listar e consultar solicitações internas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000171', 'PURCHASE_REQUEST_CREATE', 'Criar solicitações de compra', 'PURCHASE',
     'Criar solicitações internas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000172', 'PURCHASE_REQUEST_UPDATE', 'Atualizar solicitações de compra', 'PURCHASE',
     'Editar, enviar, analisar solicitações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000173', 'PURCHASE_REQUEST_APPROVE', 'Aprovar solicitações de compra', 'PURCHASE',
     'Aprovar, aprovar parcialmente ou rejeitar', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000174', 'PURCHASE_REQUEST_CANCEL', 'Cancelar solicitações de compra', 'PURCHASE',
     'Cancelar solicitação com motivo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000175', 'PURCHASE_REQUEST_CONVERT', 'Converter solicitação em cotação', 'PURCHASE',
     'Converter saldo pendente em cotação', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000176', 'PURCHASE_QUOTATION_READ', 'Consultar cotações de compra', 'PURCHASE',
     'Listar e comparar cotações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000177', 'PURCHASE_QUOTATION_CREATE', 'Criar cotações de compra', 'PURCHASE',
     'Criar cotação multipornecedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000178', 'PURCHASE_QUOTATION_UPDATE', 'Atualizar cotações de compra', 'PURCHASE',
     'Enviar, registrar respostas, selecionar', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000179', 'PURCHASE_QUOTATION_CANCEL', 'Cancelar cotações de compra', 'PURCHASE',
     'Cancelar cotação', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000180', 'PURCHASE_QUOTATION_GENERATE_PO', 'Gerar pedido a partir da cotação', 'PURCHASE',
     'Gerar um ou vários pedidos de compra', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000181', 'PURCHASE_RECEIPT_POST', 'Postar recebimento no estoque', 'PURCHASE',
     'Postagem transacional GoodsReceipt', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000182', 'SUPPLIER_RETURN_READ', 'Consultar devoluções a fornecedor', 'PURCHASE',
     'Consultar devoluções', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000183', 'SUPPLIER_RETURN_CREATE', 'Criar devoluções a fornecedor', 'PURCHASE',
     'Criar devolução', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000184', 'SUPPLIER_RETURN_UPDATE', 'Atualizar devoluções a fornecedor', 'PURCHASE',
     'Aprovar, despachar, concluir', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000185', 'SUPPLIER_RETURN_CANCEL', 'Cancelar devoluções a fornecedor', 'PURCHASE',
     'Cancelar devolução', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'PURCHASE_REQUEST_READ', 'PURCHASE_REQUEST_CREATE', 'PURCHASE_REQUEST_UPDATE',
      'PURCHASE_REQUEST_APPROVE', 'PURCHASE_REQUEST_CANCEL', 'PURCHASE_REQUEST_CONVERT',
      'PURCHASE_QUOTATION_READ', 'PURCHASE_QUOTATION_CREATE', 'PURCHASE_QUOTATION_UPDATE',
      'PURCHASE_QUOTATION_CANCEL', 'PURCHASE_QUOTATION_GENERATE_PO',
      'PURCHASE_RECEIPT_POST',
      'SUPPLIER_RETURN_READ', 'SUPPLIER_RETURN_CREATE', 'SUPPLIER_RETURN_UPDATE', 'SUPPLIER_RETURN_CANCEL'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
