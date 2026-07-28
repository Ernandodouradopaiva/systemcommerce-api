-- V257: Perfis fiscais de clientes e fornecedores (Prompt 126)
CREATE TABLE party_fiscal_profiles (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    party_type                  VARCHAR(20)     NOT NULL,
    party_id                    UUID            NOT NULL,
    store_id                    UUID            NULL,
    taxpayer_indicator          VARCHAR(40)     NOT NULL,
    state_registration          VARCHAR(30)     NULL,
    municipal_registration      VARCHAR(30)     NULL,
    suframa                     VARCHAR(20)     NULL,
    final_consumer              BOOLEAN         NOT NULL DEFAULT FALSE,
    rural_producer              BOOLEAN         NOT NULL DEFAULT FALSE,
    foreign_party               BOOLEAN         NOT NULL DEFAULT FALSE,
    country_code                VARCHAR(10)     NOT NULL DEFAULT '1058',
    ibge_city_code              VARCHAR(7)      NULL,
    fiscal_email                VARCHAR(200)    NULL,
    tax_regime                  VARCHAR(40)     NULL,
    retention_flags_json        TEXT            NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    valid_from                  DATE            NOT NULL,
    valid_until                 DATE            NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_party_fiscal_profiles PRIMARY KEY (id),
    CONSTRAINT fk_pfp_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pfp_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_pfp_party_type CHECK (party_type IN ('CUSTOMER', 'SUPPLIER')),
    CONSTRAINT ck_pfp_taxpayer CHECK (taxpayer_indicator IN (
        'CONTRIBUTOR', 'EXEMPT', 'NON_CONTRIBUTOR', 'FOREIGN'
    )),
    CONSTRAINT ck_pfp_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uk_party_fiscal_global
    ON party_fiscal_profiles (organization_id, party_type, party_id)
    WHERE store_id IS NULL AND status = 'ACTIVE' AND active = TRUE;

CREATE UNIQUE INDEX uk_party_fiscal_store
    ON party_fiscal_profiles (organization_id, party_type, party_id, store_id)
    WHERE store_id IS NOT NULL AND status = 'ACTIVE' AND active = TRUE;

CREATE INDEX idx_party_fiscal_party ON party_fiscal_profiles (party_type, party_id);

CREATE TABLE party_fiscal_histories (
    id              UUID            NOT NULL,
    party_type      VARCHAR(20)     NOT NULL,
    party_id        UUID            NOT NULL,
    profile_id      UUID            NULL,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by      UUID            NULL,
    change_type     VARCHAR(40)     NOT NULL,
    snapshot_json   TEXT            NOT NULL,
    CONSTRAINT pk_party_fiscal_histories PRIMARY KEY (id),
    CONSTRAINT fk_pfh_profile FOREIGN KEY (profile_id) REFERENCES party_fiscal_profiles (id) ON DELETE SET NULL
);

CREATE INDEX idx_party_fiscal_hist ON party_fiscal_histories (party_type, party_id, changed_at DESC);

COMMENT ON TABLE party_fiscal_profiles IS 'Perfil fiscal de cliente/fornecedor (Prompt 126); snapshot no DFe';
