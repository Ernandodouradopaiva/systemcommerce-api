-- V256: Permissões do módulo fiscal (Prompts 122–125)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000341', 'FISCAL_ESTABLISHMENT_READ', 'Consultar estabelecimento fiscal', 'FISCAL',
     'Consultar configuração fiscal por loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000342', 'FISCAL_ESTABLISHMENT_CREATE', 'Criar estabelecimento fiscal', 'FISCAL',
     'Cadastrar configuração fiscal da loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000343', 'FISCAL_ESTABLISHMENT_UPDATE', 'Atualizar estabelecimento fiscal', 'FISCAL',
     'Atualizar, ativar e inativar estabelecimento fiscal', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000344', 'FISCAL_ENVIRONMENT_CHANGE', 'Alterar ambiente fiscal', 'FISCAL',
     'Definir homologação/produção (produção exige autorização administrativa)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000345', 'FISCAL_SERIES_MANAGE', 'Gerenciar séries fiscais', 'FISCAL',
     'Definir séries padrão NF-e/NFC-e e numeração', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000346', 'FISCAL_CERTIFICATE_READ', 'Consultar certificados digitais', 'FISCAL',
     'Consultar metadados de certificados (sem segredos)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000347', 'FISCAL_CERTIFICATE_UPLOAD', 'Upload de certificado digital', 'FISCAL',
     'Enviar certificado A1 e senha cifrada', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000348', 'FISCAL_CERTIFICATE_ACTIVATE', 'Ativar certificado digital', 'FISCAL',
     'Ativar e vincular certificado ao estabelecimento', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000349', 'FISCAL_CERTIFICATE_REVOKE', 'Revogar certificado digital', 'FISCAL',
     'Revogação interna de certificado', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000034a', 'FISCAL_CERTIFICATE_TEST', 'Testar certificado digital', 'FISCAL',
     'Validar e testar assinatura do certificado', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000034b', 'FISCAL_TAX_CATALOG_READ', 'Consultar catálogos tributários', 'FISCAL',
     'Consultar NCM, CFOP, CST e demais tabelas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000034c', 'FISCAL_TAX_CATALOG_MANAGE', 'Gerenciar catálogos tributários', 'FISCAL',
     'Importar e atualizar tabelas oficiais versionadas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000034d', 'FISCAL_PRODUCT_PROFILE_READ', 'Consultar perfil fiscal do produto', 'FISCAL',
     'Consultar classificação fiscal de produtos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-00000000034e', 'FISCAL_PRODUCT_PROFILE_UPDATE', 'Atualizar perfil fiscal do produto', 'FISCAL',
     'Criar e atualizar perfil fiscal de produtos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'FISCAL_ESTABLISHMENT_READ', 'FISCAL_ESTABLISHMENT_CREATE', 'FISCAL_ESTABLISHMENT_UPDATE',
      'FISCAL_ENVIRONMENT_CHANGE', 'FISCAL_SERIES_MANAGE',
      'FISCAL_CERTIFICATE_READ', 'FISCAL_CERTIFICATE_UPLOAD', 'FISCAL_CERTIFICATE_ACTIVATE',
      'FISCAL_CERTIFICATE_REVOKE', 'FISCAL_CERTIFICATE_TEST',
      'FISCAL_TAX_CATALOG_READ', 'FISCAL_TAX_CATALOG_MANAGE',
      'FISCAL_PRODUCT_PROFILE_READ', 'FISCAL_PRODUCT_PROFILE_UPDATE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
