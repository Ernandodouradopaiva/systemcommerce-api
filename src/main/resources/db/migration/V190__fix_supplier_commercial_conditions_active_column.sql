-- V190: correção de schema — supplier_commercial_conditions (Prompt 57) não possuía a coluna
-- `active` exigida por AuditableEntity, causando falha de validação do Hibernate em toda a
-- suíte de testes. Não altera V184 (já aplicada); apenas complementa a tabela existente.

ALTER TABLE supplier_commercial_conditions
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN supplier_commercial_conditions.active IS
    'Indicador de registro ativo (padrão AuditableEntity) — ausente na criação original da tabela (V184)';
