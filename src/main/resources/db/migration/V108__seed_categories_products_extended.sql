-- V108: complementa seeds e finaliza constraints de produtos/categorias
UPDATE categories
SET status = 'ACTIVE', active = TRUE
WHERE id IN (
    'c1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000002',
    'c1000000-0000-4000-8000-000000000003'
);

INSERT INTO categories (id, name, description, parent_id, status, active, created_at, updated_at, version)
VALUES (
    'c1000000-0000-4000-8000-000000000004',
    'Periféricos',
    'Mouses, teclados e afins',
    'c1000000-0000-4000-8000-000000000001',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (id) DO NOTHING;

-- Backfill código interno a partir do SKU
UPDATE products
SET internal_code = sku
WHERE internal_code IS NULL OR LENGTH(TRIM(internal_code)) = 0;

UPDATE products
SET unit_of_measure = COALESCE(NULLIF(TRIM(unit_of_measure), ''), 'UN'),
    min_stock = COALESCE(min_stock, 0),
    allow_negative_stock = COALESCE(allow_negative_stock, FALSE),
    status = CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END;

ALTER TABLE products
    ALTER COLUMN internal_code SET NOT NULL;

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS uk_products_internal_code;

ALTER TABLE products
    ADD CONSTRAINT uk_products_internal_code UNIQUE (internal_code);

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS ck_products_internal_code_not_blank;

ALTER TABLE products
    ADD CONSTRAINT ck_products_internal_code_not_blank CHECK (LENGTH(TRIM(internal_code)) > 0);

DROP INDEX IF EXISTS uk_products_barcode_not_null;
CREATE UNIQUE INDEX uk_products_barcode_not_null
    ON products (barcode)
    WHERE barcode IS NOT NULL AND LENGTH(TRIM(barcode)) > 0;

UPDATE products SET barcode = '7891000100103', min_stock = 2, image_url = 'https://cdn.example.com/nb-001.jpg'
WHERE sku = 'NB-001';

UPDATE products SET barcode = '7891000100202', min_stock = 10, unit_of_measure = 'UN',
    category_id = 'c1000000-0000-4000-8000-000000000004'
WHERE sku = 'MS-001';

UPDATE products SET barcode = '7891000100301', min_stock = 5, unit_of_measure = 'PCT'
WHERE sku = 'PAP-A4';

UPDATE products SET barcode = NULL, min_stock = 0, unit_of_measure = 'HR', allow_negative_stock = FALSE
WHERE sku = 'SRV-SUP';
