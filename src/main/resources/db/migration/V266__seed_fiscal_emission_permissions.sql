-- V266: Permissões prompts 130–137
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000359', 'FISCAL_NUMBERING_READ', 'Consultar numeração fiscal', 'FISCAL',
     'Consultar sequências, reservas e lacunas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000035a', 'FISCAL_NUMBERING_MANAGE', 'Gerenciar numeração fiscal', 'FISCAL',
     'Reservar números e analisar lacunas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000035b', 'FISCAL_INUTILIZE', 'Inutilizar numeração fiscal', 'FISCAL',
     'Solicitar e transmitir inutilização de faixa', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000035c', 'FISCAL_SCHEMA_READ', 'Consultar schemas fiscais', 'FISCAL',
     'Consultar versões de leiaute/XSD', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000035d', 'FISCAL_SCHEMA_MANAGE', 'Gerenciar schemas fiscais', 'FISCAL',
     'Importar/atualizar XSD sem alterar documentos históricos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000035e', 'FISCAL_TRANSMISSION_READ', 'Consultar transmissões SEFAZ', 'FISCAL',
     'Consultar tentativas e status de transmissão', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000035f', 'FISCAL_TRANSMISSION_EXECUTE', 'Executar transmissão SEFAZ', 'FISCAL',
     'Status serviço e reprocessamento controlado', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000360', 'FISCAL_NFE_EMIT', 'Emitir NF-e', 'FISCAL',
     'Emitir NF-e modelo 55 a partir de venda/pedido', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000361', 'FISCAL_NFCE_EMIT', 'Emitir NFC-e', 'FISCAL',
     'Emitir NFC-e modelo 65 (PDV)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000362', 'FISCAL_DANFE_PRINT', 'Imprimir DANFE', 'FISCAL',
     'Gerar/reimprimir DANFE e DANFE NFC-e', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000363', 'FISCAL_CANCEL_REQUEST', 'Solicitar cancelamento fiscal', 'FISCAL',
     'Abrir pedido de cancelamento de DFe autorizado', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000364', 'FISCAL_CANCEL_APPROVE', 'Aprovar cancelamento fiscal', 'FISCAL',
     'Autorizar internamente cancelamento quando exigido', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'FISCAL_NUMBERING_READ', 'FISCAL_NUMBERING_MANAGE', 'FISCAL_INUTILIZE',
      'FISCAL_SCHEMA_READ', 'FISCAL_SCHEMA_MANAGE',
      'FISCAL_TRANSMISSION_READ', 'FISCAL_TRANSMISSION_EXECUTE',
      'FISCAL_NFE_EMIT', 'FISCAL_NFCE_EMIT', 'FISCAL_DANFE_PRINT',
      'FISCAL_CANCEL_REQUEST', 'FISCAL_CANCEL_APPROVE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
