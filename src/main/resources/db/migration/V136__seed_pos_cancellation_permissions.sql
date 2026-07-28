-- V136: permissões de cancelamento, estorno e devolução PDV

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000039', 'POS_CANCEL_DRAFT', 'Cancelar rascunho/suspensa no PDV', 'POS',
     'Cancelar venda em rascunho ou suspensa no PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000040', 'POS_CANCEL_COMPLETED_SALE', 'Cancelar venda concluída no PDV', 'POS',
     'Executar cancelamento de venda confirmada/paga no PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000041', 'POS_CANCEL_AUTHORIZE', 'Autorizar cancelamento no PDV', 'POS',
     'Autorizar solicitação de cancelamento de venda concluída', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000042', 'POS_REFUND_EXECUTE', 'Executar estorno no PDV', 'POS',
     'Executar e reprocessar estornos de pagamento no cancelamento PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000043', 'POS_RETURN_CREATE', 'Registrar devolução no PDV', 'POS',
     'Criar documento de devolução futura no PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

-- Operador: cancelar rascunho + criar devolução
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER', 'SELLER')
  AND p.code IN ('POS_CANCEL_DRAFT', 'POS_RETURN_CREATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Gerência: cancelamento concluído, autorização e estorno
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('POS_CANCEL_COMPLETED_SALE', 'POS_CANCEL_AUTHORIZE', 'POS_REFUND_EXECUTE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
