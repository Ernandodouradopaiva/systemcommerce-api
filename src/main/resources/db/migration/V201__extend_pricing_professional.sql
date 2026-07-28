-- V201: Tabelas de preço profissionais — tiers, grupos cliente, log (Prompt 68)
CREATE TABLE price_table_customer_groups (
    id                  UUID            NOT NULL,
    price_table_id      UUID            NOT NULL,
    customer_group_code VARCHAR(60)     NOT NULL,
    customer_group_name VARCHAR(120)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_price_table_customer_groups PRIMARY KEY (id),
    CONSTRAINT uk_ptcg UNIQUE (price_table_id, customer_group_code),
    CONSTRAINT fk_ptcg_table FOREIGN KEY (price_table_id) REFERENCES price_tables (id) ON DELETE CASCADE
);

CREATE TABLE price_tiers (
    id                  UUID            NOT NULL,
    product_price_id    UUID            NOT NULL,
    min_quantity        NUMERIC(18, 4)  NOT NULL,
    max_quantity        NUMERIC(18, 4)  NULL,
    unit_price          NUMERIC(18, 4)  NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_price_tiers PRIMARY KEY (id),
    CONSTRAINT fk_pt_product_price FOREIGN KEY (product_price_id) REFERENCES product_prices (id) ON DELETE CASCADE,
    CONSTRAINT ck_pt_qty CHECK (min_quantity > 0 AND unit_price >= 0)
);

CREATE TABLE price_resolution_logs (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    store_id            UUID            NULL,
    product_id          UUID            NOT NULL,
    channel             VARCHAR(30)     NULL,
    customer_id         UUID            NULL,
    quantity            NUMERIC(18, 4)  NULL,
    resolved_price      NUMERIC(18, 4)  NOT NULL,
    price_origin        VARCHAR(80)     NOT NULL,
    price_table_id      UUID            NULL,
    product_price_id    UUID            NULL,
    resolved_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    context_json        TEXT            NULL,
    CONSTRAINT pk_price_resolution_logs PRIMARY KEY (id),
    CONSTRAINT fk_prl_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_prl_product_at ON price_resolution_logs (product_id, resolved_at DESC);
CREATE INDEX idx_price_tiers_pp ON price_tiers (product_price_id);

ALTER TABLE product_prices
    ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS customer_id UUID NULL;

COMMENT ON TABLE price_resolution_logs IS 'Origem do preço resolvido pela API (Prompt 68)';
