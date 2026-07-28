-- V255: Perfil fiscal de produtos (Prompt 125)
CREATE TABLE product_fiscal_profiles (
    id                          UUID            NOT NULL,
    product_id                  UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    store_id                    UUID            NULL,
    uf                          CHAR(2)         NULL,
    ncm_code                    VARCHAR(10)     NOT NULL,
    cest_code                   VARCHAR(10)     NULL,
    ex_tipi                     VARCHAR(10)     NULL,
    origin_code                 VARCHAR(5)      NOT NULL,
    commercial_uom              VARCHAR(10)     NULL,
    taxable_uom                 VARCHAR(10)     NULL,
    conversion_factor           NUMERIC(19, 6)  NOT NULL DEFAULT 1,
    gtin_commercial             VARCHAR(14)     NULL,
    gtin_taxable                VARCHAR(14)     NULL,
    ipi_framing                 VARCHAR(20)     NULL,
    relevant_scale_indicator    VARCHAR(20)     NULL,
    manufacturer_cnpj           VARCHAR(14)     NULL,
    benefit_code                VARCHAR(20)     NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    valid_from                  DATE            NOT NULL,
    valid_until                 DATE            NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_fiscal_profiles PRIMARY KEY (id),
    CONSTRAINT fk_pfp_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_pfp_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pfp_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_pfp_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_pfp_factor CHECK (conversion_factor > 0)
);

CREATE INDEX idx_pfp_product ON product_fiscal_profiles (product_id, status);
CREATE INDEX idx_pfp_org_store ON product_fiscal_profiles (organization_id, store_id);
CREATE INDEX idx_pfp_validity ON product_fiscal_profiles (product_id, valid_from, valid_until);

CREATE TABLE product_fiscal_profile_stores (
    id              UUID            NOT NULL,
    profile_id      UUID            NOT NULL,
    store_id        UUID            NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_fiscal_profile_stores PRIMARY KEY (id),
    CONSTRAINT uk_product_fiscal_profile_store UNIQUE (profile_id, store_id),
    CONSTRAINT fk_pfps_profile FOREIGN KEY (profile_id) REFERENCES product_fiscal_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_pfps_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE product_tax_classifications (
    id              UUID            NOT NULL,
    profile_id      UUID            NOT NULL,
    tax_type        VARCHAR(40)     NOT NULL,
    cst_or_csosn    VARCHAR(10)     NULL,
    cfop_code       VARCHAR(10)     NULL,
    extra_json      TEXT            NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_tax_classifications PRIMARY KEY (id),
    CONSTRAINT fk_ptc_profile FOREIGN KEY (profile_id) REFERENCES product_fiscal_profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_ptc_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_ptc_profile ON product_tax_classifications (profile_id);

CREATE TABLE product_fiscal_histories (
    id              UUID            NOT NULL,
    product_id      UUID            NOT NULL,
    profile_id      UUID            NULL,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by      UUID            NULL,
    change_type     VARCHAR(40)     NOT NULL,
    snapshot_json   TEXT            NOT NULL,
    CONSTRAINT pk_product_fiscal_histories PRIMARY KEY (id),
    CONSTRAINT fk_pfh_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_pfh_profile FOREIGN KEY (profile_id) REFERENCES product_fiscal_profiles (id) ON DELETE SET NULL
);

CREATE INDEX idx_pfh_product ON product_fiscal_histories (product_id, changed_at DESC);

COMMENT ON TABLE product_fiscal_profiles IS 'Perfil fiscal versionado do produto (Prompt 125) — snapshot na emissão';
COMMENT ON COLUMN product_fiscal_profiles.conversion_factor IS 'Conversão comercial → tributável calculada na API';
