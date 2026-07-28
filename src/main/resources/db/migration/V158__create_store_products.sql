-- V158: StoreProduct — disponibilidade comercial produto × loja

CREATE TABLE IF NOT EXISTS store_products (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores (id),
    product_id UUID NOT NULL REFERENCES products (id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    allows_sale BOOLEAN NOT NULL DEFAULT TRUE,
    allows_pos_sale BOOLEAN NOT NULL DEFAULT TRUE,
    allows_erp_sale BOOLEAN NOT NULL DEFAULT TRUE,
    local_internal_code VARCHAR(60),
    local_barcode VARCHAR(60),
    local_default_price NUMERIC(19, 2),
    local_min_stock NUMERIC(19, 3),
    local_max_stock NUMERIC(19, 3),
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    physical_location VARCHAR(120),
    aisle VARCHAR(40),
    shelf VARCHAR(40),
    display_position VARCHAR(80),
    commercialization_start DATE,
    commercialization_end DATE,
    block_reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_store_products_store_product UNIQUE (store_id, product_id),
    CONSTRAINT ck_store_products_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_store_products_store_internal_code
    ON store_products (store_id, local_internal_code)
    WHERE local_internal_code IS NOT NULL AND LENGTH(TRIM(local_internal_code)) > 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_store_products_store_barcode
    ON store_products (store_id, local_barcode)
    WHERE local_barcode IS NOT NULL AND LENGTH(TRIM(local_barcode)) > 0;

CREATE INDEX IF NOT EXISTS idx_store_products_store ON store_products (store_id);
CREATE INDEX IF NOT EXISTS idx_store_products_product ON store_products (product_id);
CREATE INDEX IF NOT EXISTS idx_store_products_status ON store_products (store_id, status);

COMMENT ON TABLE store_products IS 'Configuração comercial do produto por loja (cadastro global permanece em products)';

-- Seed: habilita produtos existentes nas lojas LOJA-01 e LOJA-02
INSERT INTO store_products (
    id, store_id, product_id, status, allows_sale, allows_pos_sale, allows_erp_sale,
    allow_negative_stock, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    s.id,
    p.id,
    'ACTIVE',
    TRUE,
    TRUE,
    TRUE,
    COALESCE(p.allow_negative_stock, FALSE),
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
CROSS JOIN products p
WHERE s.code IN ('LOJA-01', 'LOJA-02')
  AND p.active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM store_products sp WHERE sp.store_id = s.id AND sp.product_id = p.id
  );
