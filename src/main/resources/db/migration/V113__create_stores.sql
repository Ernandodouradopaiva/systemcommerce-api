-- V113: lojas / estabelecimentos (PDV)
CREATE TABLE stores (
    id                  UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    trade_name          VARCHAR(200)    NULL,
    document            VARCHAR(20)     NULL,
    state_registration  VARCHAR(30)     NULL,
    email               VARCHAR(255)    NULL,
    phone               VARCHAR(30)     NULL,
    zip_code            VARCHAR(10)     NULL,
    street              VARCHAR(200)    NULL,
    number              VARCHAR(20)     NULL,
    complement          VARCHAR(100)    NULL,
    district            VARCHAR(100)    NULL,
    city                VARCHAR(100)    NULL,
    state               VARCHAR(2)      NULL,
    timezone            VARCHAR(64)     NOT NULL DEFAULT 'America/Sao_Paulo',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_stores PRIMARY KEY (id),
    CONSTRAINT uk_stores_code UNIQUE (code),
    CONSTRAINT ck_stores_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_stores_code_not_blank CHECK (LENGTH(TRIM(code)) > 0)
);

CREATE INDEX idx_stores_status ON stores (status);
CREATE INDEX idx_stores_name ON stores (name);
CREATE INDEX idx_stores_active ON stores (active);

COMMENT ON TABLE stores IS 'Lojas / estabelecimentos comerciais';
COMMENT ON COLUMN stores.code IS 'Código único da loja';
COMMENT ON COLUMN stores.timezone IS 'Fuso horário IANA da loja (ex.: America/Sao_Paulo)';
