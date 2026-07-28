-- V261: Permissões prompts 126–129
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-00000000034f', 'FISCAL_PARTY_PROFILE_READ', 'Consultar perfil fiscal de partes', 'FISCAL',
     'Consultar perfil fiscal de clientes e fornecedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000350', 'FISCAL_PARTY_PROFILE_UPDATE', 'Atualizar perfil fiscal de partes', 'FISCAL',
     'Criar e atualizar perfil fiscal de clientes/fornecedores', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000351', 'FISCAL_TAX_ENGINE_READ', 'Consultar cálculos fiscais', 'FISCAL',
     'Consultar simulações e cálculos do motor tributário', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000352', 'FISCAL_TAX_ENGINE_CALCULATE', 'Calcular tributos', 'FISCAL',
     'Executar simulação/cálculo no motor tributário', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000353', 'FISCAL_TAX_RULE_MANAGE', 'Gerenciar regras tributárias', 'FISCAL',
     'Criar e versionar regras do motor fiscal', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000354', 'FISCAL_OPERATION_READ', 'Consultar operações fiscais', 'FISCAL',
     'Consultar naturezas/operações fiscais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000355', 'FISCAL_OPERATION_MANAGE', 'Gerenciar operações fiscais', 'FISCAL',
     'Cadastrar e versionar operações fiscais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000356', 'FISCAL_DOCUMENT_READ', 'Consultar documentos fiscais', 'FISCAL',
     'Consultar DFe (NF-e/NFC-e) e status', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000357', 'FISCAL_DOCUMENT_CREATE', 'Criar documentos fiscais', 'FISCAL',
     'Criar rascunhos de DFe', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000358', 'FISCAL_DOCUMENT_UPDATE', 'Atualizar documentos fiscais', 'FISCAL',
     'Atualizar rascunhos e transicionar status (não altera autorizados)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'FISCAL_PARTY_PROFILE_READ', 'FISCAL_PARTY_PROFILE_UPDATE',
      'FISCAL_TAX_ENGINE_READ', 'FISCAL_TAX_ENGINE_CALCULATE', 'FISCAL_TAX_RULE_MANAGE',
      'FISCAL_OPERATION_READ', 'FISCAL_OPERATION_MANAGE',
      'FISCAL_DOCUMENT_READ', 'FISCAL_DOCUMENT_CREATE', 'FISCAL_DOCUMENT_UPDATE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
