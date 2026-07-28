-- V198: Marcas, fabricantes e linhas (Prompt 65)
CREATE TABLE brands (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(2000)   NULL,
    country_code        VARCHAR(2)      NULL,
    website             VARCHAR(255)    NULL,
    logo_url            VARCHAR(500)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uk_brands_org_code UNIQUE (organization_id, code),
    CONSTRAINT uk_brands_org_name UNIQUE (organization_id, name),
    CONSTRAINT fk_brands_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_brands_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE manufacturers (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(2000)   NULL,
    country_code        VARCHAR(2)      NULL,
    website             VARCHAR(255)    NULL,
    logo_url            VARCHAR(500)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_manufacturers PRIMARY KEY (id),
    CONSTRAINT uk_manufacturers_org_code UNIQUE (organization_id, code),
    CONSTRAINT uk_manufacturers_org_name UNIQUE (organization_id, name),
    CONSTRAINT fk_manufacturers_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_manufacturers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE product_lines (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    brand_id            UUID            NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(2000)   NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_lines PRIMARY KEY (id),
    CONSTRAINT uk_product_lines_org_code UNIQUE (organization_id, code),
    CONSTRAINT uk_product_lines_org_name UNIQUE (organization_id, name),
    CONSTRAINT fk_product_lines_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_product_lines_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT ck_product_lines_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS brand_id UUID NULL,
    ADD COLUMN IF NOT EXISTS manufacturer_id UUID NULL,
    ADD COLUMN IF NOT EXISTS product_line_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_brand') THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_manufacturer') THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_manufacturer FOREIGN KEY (manufacturer_id) REFERENCES manufacturers (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_product_line') THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_product_line FOREIGN KEY (product_line_id) REFERENCES product_lines (id);
    END IF;
END $$;

CREATE INDEX idx_products_brand ON products (brand_id);
CREATE INDEX idx_products_manufacturer ON products (manufacturer_id);
CREATE INDEX idx_products_line ON products (product_line_id);
