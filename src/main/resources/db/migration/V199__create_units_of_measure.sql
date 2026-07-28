-- V199: Unidades de medida e conversões (Prompt 66)
CREATE TABLE units_of_measure (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    code                VARCHAR(20)     NOT NULL,
    name                VARCHAR(80)     NOT NULL,
    description         VARCHAR(500)    NULL,
    symbol              VARCHAR(20)     NULL,
    precision_scale     INT             NOT NULL DEFAULT 4,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    system_unit         BOOLEAN         NOT NULL DEFAULT FALSE,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_units_of_measure PRIMARY KEY (id),
    CONSTRAINT uk_uom_code UNIQUE (code),
    CONSTRAINT ck_uom_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_uom_precision CHECK (precision_scale >= 0 AND precision_scale <= 8)
);

CREATE TABLE unit_conversions (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    from_unit_id        UUID            NOT NULL,
    to_unit_id          UUID            NOT NULL,
    factor              NUMERIC(24, 10) NOT NULL,
    rounding_mode       VARCHAR(30)     NOT NULL DEFAULT 'HALF_UP',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_unit_conversions PRIMARY KEY (id),
    CONSTRAINT uk_unit_conversions UNIQUE (from_unit_id, to_unit_id),
    CONSTRAINT fk_uc_from FOREIGN KEY (from_unit_id) REFERENCES units_of_measure (id),
    CONSTRAINT fk_uc_to FOREIGN KEY (to_unit_id) REFERENCES units_of_measure (id),
    CONSTRAINT ck_uc_factor CHECK (factor > 0),
    CONSTRAINT ck_uc_rounding CHECK (rounding_mode IN ('HALF_UP', 'HALF_DOWN', 'UP', 'DOWN', 'CEILING', 'FLOOR'))
);

CREATE TABLE product_units (
    id                  UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    stock_unit_id       UUID            NOT NULL,
    purchase_unit_id    UUID            NOT NULL,
    sales_unit_id       UUID            NOT NULL,
    purchase_to_stock_factor NUMERIC(24, 10) NOT NULL DEFAULT 1,
    sales_to_stock_factor   NUMERIC(24, 10) NOT NULL DEFAULT 1,
    rounding_mode       VARCHAR(30)     NOT NULL DEFAULT 'HALF_UP',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_units PRIMARY KEY (id),
    CONSTRAINT uk_product_units_product UNIQUE (product_id),
    CONSTRAINT fk_pu_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_pu_stock FOREIGN KEY (stock_unit_id) REFERENCES units_of_measure (id),
    CONSTRAINT fk_pu_purchase FOREIGN KEY (purchase_unit_id) REFERENCES units_of_measure (id),
    CONSTRAINT fk_pu_sales FOREIGN KEY (sales_unit_id) REFERENCES units_of_measure (id),
    CONSTRAINT ck_pu_factors CHECK (purchase_to_stock_factor > 0 AND sales_to_stock_factor > 0)
);

CREATE TABLE supplier_product_units (
    id                  UUID            NOT NULL,
    supplier_id         UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    unit_id             UUID            NOT NULL,
    factor_to_stock     NUMERIC(24, 10) NOT NULL DEFAULT 1,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_product_units PRIMARY KEY (id),
    CONSTRAINT uk_spu UNIQUE (supplier_id, product_id, unit_id),
    CONSTRAINT fk_spu_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_spu_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_spu_unit FOREIGN KEY (unit_id) REFERENCES units_of_measure (id),
    CONSTRAINT ck_spu_factor CHECK (factor_to_stock > 0)
);

CREATE TABLE sales_product_units (
    id                  UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    unit_id             UUID            NOT NULL,
    factor_to_stock     NUMERIC(24, 10) NOT NULL DEFAULT 1,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sales_product_units PRIMARY KEY (id),
    CONSTRAINT uk_sales_pu UNIQUE (product_id, unit_id),
    CONSTRAINT fk_sales_pu_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_sales_pu_unit FOREIGN KEY (unit_id) REFERENCES units_of_measure (id),
    CONSTRAINT ck_sales_pu_factor CHECK (factor_to_stock > 0)
);

-- Seed unidades do sistema
INSERT INTO units_of_measure (id, organization_id, code, name, symbol, precision_scale, system_unit, status, active, created_at, updated_at, version)
VALUES
    ('b1000000-0000-4000-8000-000000000001', NULL, 'UN', 'Unidade', 'un', 0, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000002', NULL, 'CX', 'Caixa', 'cx', 0, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000003', NULL, 'KG', 'Quilograma', 'kg', 3, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000004', NULL, 'G', 'Grama', 'g', 0, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000005', NULL, 'L', 'Litro', 'L', 3, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000006', NULL, 'ML', 'Mililitro', 'ml', 0, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000007', NULL, 'M', 'Metro', 'm', 3, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000008', NULL, 'M2', 'Metro quadrado', 'm²', 3, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000009', NULL, 'PCT', 'Pacote', 'pct', 0, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO unit_conversions (id, from_unit_id, to_unit_id, factor, rounding_mode, active, created_at, updated_at, version)
SELECT 'b1000000-0000-4000-8000-000000000011',
       (SELECT id FROM units_of_measure WHERE code = 'CX'),
       (SELECT id FROM units_of_measure WHERE code = 'UN'),
       12, 'HALF_UP', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
WHERE NOT EXISTS (SELECT 1 FROM unit_conversions uc
    JOIN units_of_measure f ON f.id = uc.from_unit_id AND f.code = 'CX'
    JOIN units_of_measure t ON t.id = uc.to_unit_id AND t.code = 'UN');

ALTER TABLE stock_movements
    ADD COLUMN IF NOT EXISTS informed_unit_code VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS conversion_factor NUMERIC(24, 10) NULL,
    ADD COLUMN IF NOT EXISTS base_quantity NUMERIC(18, 4) NULL;

COMMENT ON TABLE units_of_measure IS 'Unidades de medida (Prompt 66); estoque sempre na unidade-base';
