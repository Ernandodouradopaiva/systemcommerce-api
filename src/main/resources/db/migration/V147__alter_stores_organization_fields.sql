-- V147: lojas vinculadas à organização + cadastro completo
ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS organization_id UUID NULL,
    ADD COLUMN IF NOT EXISTS municipal_registration VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS establishment_type VARCHAR(40) NOT NULL DEFAULT 'BRANCH',
    ADD COLUMN IF NOT EXISTS is_headquarters BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS opening_date DATE NULL,
    ADD COLUMN IF NOT EXISTS allows_sales BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allows_pos BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE stores
SET organization_id = 'b1000000-0000-4000-8000-000000000001'
WHERE organization_id IS NULL;

UPDATE stores
SET is_headquarters = TRUE,
    establishment_type = 'HEADQUARTERS'
WHERE code = 'LOJA-01'
  AND organization_id = 'b1000000-0000-4000-8000-000000000001';

ALTER TABLE stores
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE stores
    DROP CONSTRAINT IF EXISTS uk_stores_code;

ALTER TABLE stores
    DROP CONSTRAINT IF EXISTS fk_stores_organization;

ALTER TABLE stores
    ADD CONSTRAINT fk_stores_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id);

ALTER TABLE stores
    DROP CONSTRAINT IF EXISTS uk_stores_organization_code;

ALTER TABLE stores
    ADD CONSTRAINT uk_stores_organization_code UNIQUE (organization_id, code);

ALTER TABLE stores
    DROP CONSTRAINT IF EXISTS ck_stores_establishment_type;

ALTER TABLE stores
    ADD CONSTRAINT ck_stores_establishment_type CHECK (
        establishment_type IN (
            'HEADQUARTERS',
            'BRANCH',
            'DISTRIBUTION_CENTER',
            'VIRTUAL_STORE',
            'OTHER'
        )
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_stores_document
    ON stores (document)
    WHERE document IS NOT NULL AND LENGTH(TRIM(document)) > 0;

CREATE INDEX IF NOT EXISTS idx_stores_organization ON stores (organization_id);
CREATE INDEX IF NOT EXISTS idx_stores_establishment_type ON stores (establishment_type);
CREATE INDEX IF NOT EXISTS idx_stores_is_headquarters ON stores (organization_id, is_headquarters)
    WHERE is_headquarters = TRUE;
CREATE INDEX IF NOT EXISTS idx_stores_allows_sales ON stores (allows_sales);
CREATE INDEX IF NOT EXISTS idx_stores_allows_pos ON stores (allows_pos);

COMMENT ON COLUMN stores.organization_id IS 'Organização dona da loja';
COMMENT ON COLUMN stores.establishment_type IS 'Tipo: HEADQUARTERS, BRANCH, DISTRIBUTION_CENTER, VIRTUAL_STORE, OTHER';
COMMENT ON COLUMN stores.is_headquarters IS 'Marca a loja matriz da organização';
COMMENT ON COLUMN stores.allows_sales IS 'Se false, bloqueia novas vendas';
COMMENT ON COLUMN stores.allows_pos IS 'Se false, bloqueia abertura de caixa / operação PDV';
COMMENT ON COLUMN stores.document IS 'CNPJ da loja (único quando informado)';
