-- V212: Kits, combos e produtos compostos (Prompt 78)
CREATE TABLE product_bundles (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    bundle_type             VARCHAR(40)     NOT NULL,
    code                    VARCHAR(40)     NOT NULL,
    name                    VARCHAR(200)    NOT NULL,
    description             VARCHAR(2000)   NULL,
    price_policy            VARCHAR(40)     NOT NULL DEFAULT 'FIXED',
    inventory_policy        VARCHAR(40)     NOT NULL DEFAULT 'COMPONENTS',
    fixed_price             NUMERIC(18, 4)  NULL,
    component_discount_pct  NUMERIC(7, 4)   NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_bundles PRIMARY KEY (id),
    CONSTRAINT uk_product_bundles_code UNIQUE (organization_id, code),
    CONSTRAINT uk_product_bundles_product UNIQUE (product_id),
    CONSTRAINT fk_pbundle_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pbundle_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_pbundle_type CHECK (bundle_type IN (
        'COMMERCIAL_KIT', 'PROMO_COMBO', 'COMPOUND', 'MULTI_PACK', 'SET'
    )),
    CONSTRAINT ck_pbundle_price CHECK (price_policy IN (
        'FIXED', 'SUM_COMPONENTS', 'DISCOUNT_ON_COMPONENTS'
    )),
    CONSTRAINT ck_pbundle_inv CHECK (inventory_policy IN (
        'COMPONENTS', 'PREASSEMBLED', 'COMPONENTS_ON_SALE'
    )),
    CONSTRAINT ck_pbundle_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE product_bundle_items (
    id                      UUID            NOT NULL,
    product_bundle_id       UUID            NOT NULL,
    component_product_id    UUID            NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    line_number             INT             NOT NULL,
    optional_component      BOOLEAN         NOT NULL DEFAULT FALSE,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_bundle_items PRIMARY KEY (id),
    CONSTRAINT uk_pbi_line UNIQUE (product_bundle_id, line_number),
    CONSTRAINT uk_pbi_component UNIQUE (product_bundle_id, component_product_id),
    CONSTRAINT fk_pbi_bundle FOREIGN KEY (product_bundle_id) REFERENCES product_bundles (id) ON DELETE CASCADE,
    CONSTRAINT fk_pbi_component FOREIGN KEY (component_product_id) REFERENCES products (id),
    CONSTRAINT ck_pbi_qty CHECK (quantity > 0)
);

CREATE TABLE bundle_price_policies (
    id                      UUID            NOT NULL,
    product_bundle_id       UUID            NOT NULL,
    channel                 VARCHAR(30)     NULL,
    store_id                UUID            NULL,
    price_policy            VARCHAR(40)     NOT NULL,
    fixed_price             NUMERIC(18, 4)  NULL,
    discount_percent        NUMERIC(7, 4)   NULL,
    valid_from              DATE            NULL,
    valid_until             DATE            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_bundle_price_policies PRIMARY KEY (id),
    CONSTRAINT fk_bpp_bundle FOREIGN KEY (product_bundle_id) REFERENCES product_bundles (id) ON DELETE CASCADE,
    CONSTRAINT fk_bpp_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE bundle_inventory_policies (
    id                      UUID            NOT NULL,
    product_bundle_id       UUID            NOT NULL,
    warehouse_id            UUID            NULL,
    inventory_policy        VARCHAR(40)     NOT NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_bundle_inventory_policies PRIMARY KEY (id),
    CONSTRAINT fk_bip_bundle FOREIGN KEY (product_bundle_id) REFERENCES product_bundles (id) ON DELETE CASCADE,
    CONSTRAINT fk_bip_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

COMMENT ON TABLE product_bundles IS 'Kits/combos — disponibilidade e preço só na API (Prompt 78)';
