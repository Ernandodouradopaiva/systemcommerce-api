-- V277: Permissões prompts 143–148
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000371', 'FISCAL_DFE_DISTRIBUTION_READ', 'Consultar distribuição DFe', 'FISCAL',
     'Consultar NSU e documentos destinados', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000372', 'FISCAL_DFE_DISTRIBUTION_QUERY', 'Consultar SEFAZ distribuição', 'FISCAL',
     'Executar busca incremental NSU', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000373', 'FISCAL_MANIFESTATION_READ', 'Consultar manifestações', 'FISCAL',
     'Histórico de manifestação do destinatário', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000374', 'FISCAL_MANIFESTATION_MANAGE', 'Gerenciar manifestações', 'FISCAL',
     'Ciência, confirmação, desconhecimento, operação não realizada', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000375', 'FISCAL_MONITOR_READ', 'Monitor fiscal', 'FISCAL',
     'Fila, status e ações operacionais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000376', 'FISCAL_DOCUMENT_TRANSMIT', 'Retransmitir documento fiscal', 'FISCAL',
     'Retransmissão segura após consulta de situação', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000377', 'FISCAL_DOCUMENT_CANCEL', 'Cancelar documento fiscal', 'FISCAL',
     'Solicitar/aprovar cancelamento via monitor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000378', 'FISCAL_DOCUMENT_CORRECT', 'Corrigir documento fiscal', 'FISCAL',
     'CC-e e correção de rascunho', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000379', 'FISCAL_DOCUMENT_VOID_NUMBER', 'Inutilizar numeração', 'FISCAL',
     'Inutilização de faixa numérica', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000037a', 'FISCAL_XML_DOWNLOAD', 'Download de XML fiscal', 'FISCAL',
     'Baixar XML e artefatos armazenados', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000037b', 'FISCAL_CONFIGURATION_MANAGE', 'Configuração fiscal global', 'FISCAL',
     'Ambiente, versões de leiaute e parâmetros', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000037c', 'FISCAL_GLOBAL_ACCESS', 'Acesso fiscal global', 'FISCAL',
     'Auditoria e visão cross-loja autorizada', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000037d', 'FISCAL_REPORT_READ', 'Relatórios e dashboard fiscal', 'FISCAL',
     'Indicadores e exportações fiscais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000037e', 'FISCAL_STORAGE_MANAGE', 'Gerenciar storage fiscal', 'FISCAL',
     'Políticas de retenção e exportação em lote', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'FISCAL_DFE_DISTRIBUTION_READ', 'FISCAL_DFE_DISTRIBUTION_QUERY',
      'FISCAL_MANIFESTATION_READ', 'FISCAL_MANIFESTATION_MANAGE',
      'FISCAL_MONITOR_READ', 'FISCAL_DOCUMENT_TRANSMIT', 'FISCAL_DOCUMENT_CANCEL',
      'FISCAL_DOCUMENT_CORRECT', 'FISCAL_DOCUMENT_VOID_NUMBER',
      'FISCAL_XML_DOWNLOAD', 'FISCAL_CONFIGURATION_MANAGE', 'FISCAL_GLOBAL_ACCESS',
      'FISCAL_REPORT_READ', 'FISCAL_STORAGE_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
