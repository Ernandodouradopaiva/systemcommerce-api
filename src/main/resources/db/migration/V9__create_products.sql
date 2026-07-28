-- V9: produtos
CREATE TABLE products (
    id              UUID            NOT NULL,
    sku             VARCHAR(60)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     VARCHAR(1000)   NULL,
    category_id     UUID            NOT NULL,
    unit_price      NUMERIC(19, 2)  NOT NULL,
    cost_price      NUMERIC(19, 2)  NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_sku_not_blank CHECK (LENGTH(TRIM(sku)) > 0),
    CONSTRAINT ck_products_name_not_blank CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_products_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_products_cost_price_non_negative CHECK (cost_price IS NULL OR cost_price >= 0)
);

CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_active ON products (active);

COMMENT ON TABLE products IS 'Catálogo de produtos comercializáveis';
COMMENT ON COLUMN products.unit_price IS 'Preço de venda base (BigDecimal / NUMERIC)';
COMMENT ON COLUMN products.cost_price IS 'Custo opcional do produto';
