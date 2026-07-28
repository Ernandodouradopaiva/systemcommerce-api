-- V279: Migração / histórico externo + estado pós-autorização (Prompt 150)
ALTER TABLE fiscal_documents
    ADD COLUMN IF NOT EXISTS external_import BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS source_system VARCHAR(60) NULL,
    ADD COLUMN IF NOT EXISTS migration_batch_id UUID NULL;

CREATE INDEX IF NOT EXISTS idx_fd_migration_batch ON fiscal_documents (migration_batch_id);
CREATE INDEX IF NOT EXISTS idx_fd_external_import ON fiscal_documents (external_import) WHERE external_import = TRUE;

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-00000000037f', 'FISCAL_HISTORY_IMPORT', 'Importar histórico fiscal externo', 'FISCAL',
     'Importar DFe legado sem emissão retroativa automática', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000380', 'FISCAL_MIGRATION_MANAGE', 'Gerenciar migração fiscal', 'FISCAL',
     'Planos de migração, lotes e validação de cutover', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('FISCAL_HISTORY_IMPORT', 'FISCAL_MIGRATION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
