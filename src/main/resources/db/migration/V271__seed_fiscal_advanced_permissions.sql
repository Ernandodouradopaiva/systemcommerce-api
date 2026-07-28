-- V271: Permissões prompts 138–142
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000365', 'FISCAL_CCE_READ', 'Consultar CC-e', 'FISCAL',
     'Consultar cartas de correção eletrônica', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000366', 'FISCAL_CCE_REQUEST', 'Solicitar CC-e', 'FISCAL',
     'Criar carta de correção para NF-e autorizada', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000367', 'FISCAL_CCE_TRANSMIT', 'Transmitir CC-e', 'FISCAL',
     'Transmitir evento de correção à SEFAZ', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000368', 'FISCAL_CONTINGENCY_READ', 'Consultar contingência fiscal', 'FISCAL',
     'Monitorar contingência e documentos pendentes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000369', 'FISCAL_CONTINGENCY_MANAGE', 'Gerenciar contingência fiscal', 'FISCAL',
     'Ativar/encerrar contingência e retransmitir', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000036a', 'FISCAL_SPECIAL_EMIT', 'Emitir documentos fiscais especiais', 'FISCAL',
     'Complementar, ajuste, remessa, retorno, anulação', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000036b', 'FISCAL_RETURN_READ', 'Consultar devoluções fiscais', 'FISCAL',
     'Consultar vínculos de devolução fiscal', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000036c', 'FISCAL_RETURN_EMIT', 'Emitir devolução fiscal', 'FISCAL',
     'Emitir NF de devolução de venda/compra', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000036d', 'FISCAL_INCOMING_READ', 'Consultar entrada fiscal', 'FISCAL',
     'Consultar XML de fornecedor importados', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000036e', 'FISCAL_INCOMING_IMPORT', 'Importar XML de entrada', 'FISCAL',
     'Importar XML de NF-e de fornecedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000036f', 'FISCAL_INCOMING_LINK', 'Vincular entrada fiscal', 'FISCAL',
     'Vincular XML a pedido/recebimento/fornecedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000370', 'FISCAL_INCOMING_MANIFEST', 'Manifestação do destinatário', 'FISCAL',
     'Manifestar ciência/confirmação sobre DFe de terceiros', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'FISCAL_CCE_READ', 'FISCAL_CCE_REQUEST', 'FISCAL_CCE_TRANSMIT',
      'FISCAL_CONTINGENCY_READ', 'FISCAL_CONTINGENCY_MANAGE',
      'FISCAL_SPECIAL_EMIT',
      'FISCAL_RETURN_READ', 'FISCAL_RETURN_EMIT',
      'FISCAL_INCOMING_READ', 'FISCAL_INCOMING_IMPORT', 'FISCAL_INCOMING_LINK', 'FISCAL_INCOMING_MANIFEST'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
