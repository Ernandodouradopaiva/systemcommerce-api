-- V252: Estabelecimentos fiscais por loja (Prompt 122)
CREATE TABLE fiscal_establishments (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    legal_name              VARCHAR(200)    NOT NULL,
    trade_name              VARCHAR(200)    NULL,
    cnpj                    VARCHAR(14)     NOT NULL,
    state_registration      VARCHAR(30)     NULL,
    municipal_registration  VARCHAR(30)     NULL,
    cnae_principal          VARCHAR(10)     NULL,
    ibge_city_code          VARCHAR(7)      NOT NULL,
    uf                      CHAR(2)         NOT NULL,
    zip_code                VARCHAR(8)      NULL,
    street                  VARCHAR(200)    NULL,
    number                  VARCHAR(20)     NULL,
    complement              VARCHAR(100)    NULL,
    district                VARCHAR(100)    NULL,
    city                    VARCHAR(120)    NULL,
    phone                   VARCHAR(30)     NULL,
    email                   VARCHAR(200)    NULL,
    tax_regime              VARCHAR(40)     NOT NULL,
    crt                     SMALLINT        NOT NULL,
    taxpayer_indicator      VARCHAR(40)     NOT NULL,
    fiscal_environment      VARCHAR(20)     NOT NULL DEFAULT 'HOMOLOGATION',
    default_nfe_series      VARCHAR(10)     NULL,
    default_nfce_series     VARCHAR(10)     NULL,
    allows_nfe              BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_nfce             BOOLEAN         NOT NULL DEFAULT TRUE,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    accreditation_date      DATE            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_establishments PRIMARY KEY (id),
    CONSTRAINT uk_fiscal_est_store UNIQUE (store_id),
    CONSTRAINT fk_fe_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_fe_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_fe_env CHECK (fiscal_environment IN ('HOMOLOGATION', 'PRODUCTION')),
    CONSTRAINT ck_fe_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_fe_crt CHECK (crt IN (1, 2, 3)),
    CONSTRAINT ck_fe_uf CHECK (uf ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_fe_cnpj CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_fe_ibge CHECK (ibge_city_code ~ '^[0-9]{7}$')
);

CREATE INDEX idx_fe_org ON fiscal_establishments (organization_id);
CREATE INDEX idx_fe_cnpj ON fiscal_establishments (cnpj);
CREATE INDEX idx_fe_status ON fiscal_establishments (organization_id, status);

CREATE TABLE fiscal_establishment_history (
    id                  UUID            NOT NULL,
    establishment_id    UUID            NOT NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    change_type         VARCHAR(40)     NOT NULL,
    snapshot_json       TEXT            NOT NULL,
    CONSTRAINT pk_fiscal_est_history PRIMARY KEY (id),
    CONSTRAINT fk_feh_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id) ON DELETE CASCADE
);

CREATE INDEX idx_feh_est ON fiscal_establishment_history (establishment_id, changed_at DESC);

CREATE TABLE fiscal_numbering_series (
    id                  UUID            NOT NULL,
    establishment_id    UUID            NOT NULL,
    model               VARCHAR(10)     NOT NULL,
    series              VARCHAR(10)     NOT NULL,
    environment         VARCHAR(20)     NOT NULL,
    next_number         BIGINT          NOT NULL DEFAULT 1,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_numbering_series PRIMARY KEY (id),
    CONSTRAINT uk_fns_est_model_series_env UNIQUE (establishment_id, model, series, environment),
    CONSTRAINT fk_fns_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id) ON DELETE CASCADE,
    CONSTRAINT ck_fns_model CHECK (model IN ('55', '65')),
    CONSTRAINT ck_fns_env CHECK (environment IN ('HOMOLOGATION', 'PRODUCTION')),
    CONSTRAINT ck_fns_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_fns_next CHECK (next_number >= 1)
);

CREATE INDEX idx_fns_est ON fiscal_numbering_series (establishment_id, environment);

COMMENT ON TABLE fiscal_establishments IS 'Configuração fiscal por loja (Prompt 122) — snapshot do emitente na emissão';
COMMENT ON TABLE fiscal_numbering_series IS 'Séries de numeração por modelo (55/65) e ambiente';
