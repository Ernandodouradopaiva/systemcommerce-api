-- V22: ampliação do cadastro de produtos (colunas novas; NOT NULL do internal_code em V108 após seeds)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS internal_code VARCHAR(60),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(60),
    ADD COLUMN IF NOT EXISTS unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'UN',
    ADD COLUMN IF NOT EXISTS min_stock NUMERIC(19, 3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

UPDATE products
SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END
WHERE status IS NULL OR status = 'ACTIVE';

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS ck_products_status;

ALTER TABLE products
    ADD CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS ck_products_min_stock_non_negative;

ALTER TABLE products
    ADD CONSTRAINT ck_products_min_stock_non_negative CHECK (min_stock >= 0);

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS ck_products_uom_not_blank;

ALTER TABLE products
    ADD CONSTRAINT ck_products_uom_not_blank CHECK (LENGTH(TRIM(unit_of_measure)) > 0);

CREATE INDEX IF NOT EXISTS idx_products_barcode ON products (barcode);
CREATE INDEX IF NOT EXISTS idx_products_status ON products (status);
CREATE INDEX IF NOT EXISTS idx_products_internal_code ON products (internal_code);

COMMENT ON COLUMN products.internal_code IS 'Código interno único do produto';
COMMENT ON COLUMN products.barcode IS 'Código de barras (único quando informado)';
COMMENT ON COLUMN products.unit_of_measure IS 'Unidade de medida (UN, KG, CX, etc.)';
COMMENT ON COLUMN products.min_stock IS 'Estoque mínimo (NUMERIC)';
COMMENT ON COLUMN products.allow_negative_stock IS 'Permite estoque negativo';
COMMENT ON COLUMN products.status IS 'ACTIVE | INACTIVE';
COMMENT ON COLUMN products.image_url IS 'URL da imagem do produto';
