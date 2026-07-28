-- V146: organização (empresa) dona das lojas
CREATE TABLE organizations (
    id                      UUID            NOT NULL,
    code                    VARCHAR(40)     NOT NULL,
    legal_name              VARCHAR(200)    NOT NULL,
    trade_name              VARCHAR(200)    NULL,
    document                VARCHAR(20)     NULL,
    state_registration      VARCHAR(30)     NULL,
    municipal_registration  VARCHAR(30)     NULL,
    email                   VARCHAR(255)    NULL,
    phone                   VARCHAR(30)     NULL,
    website                 VARCHAR(255)    NULL,
    zip_code                VARCHAR(10)     NULL,
    street                  VARCHAR(200)    NULL,
    number                  VARCHAR(20)     NULL,
    complement              VARCHAR(100)    NULL,
    district                VARCHAR(100)    NULL,
    city                    VARCHAR(100)    NULL,
    state                   VARCHAR(2)      NULL,
    default_timezone        VARCHAR(64)     NOT NULL DEFAULT 'America/Sao_Paulo',
    currency                VARCHAR(3)      NOT NULL DEFAULT 'BRL',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_organizations PRIMARY KEY (id),
    CONSTRAINT uk_organizations_code UNIQUE (code),
    CONSTRAINT ck_organizations_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_organizations_code_not_blank CHECK (LENGTH(TRIM(code)) > 0),
    CONSTRAINT ck_organizations_currency CHECK (LENGTH(TRIM(currency)) = 3)
);

CREATE UNIQUE INDEX uk_organizations_document
    ON organizations (document)
    WHERE document IS NOT NULL AND LENGTH(TRIM(document)) > 0;

CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_organizations_active ON organizations (active);
CREATE INDEX idx_organizations_legal_name ON organizations (legal_name);

COMMENT ON TABLE organizations IS 'Organização / empresa proprietária das lojas (isolamento lógico)';
COMMENT ON COLUMN organizations.document IS 'CNPJ da organização (único quando informado)';
COMMENT ON COLUMN organizations.default_timezone IS 'Fuso IANA padrão (ex.: America/Sao_Paulo)';
COMMENT ON COLUMN organizations.currency IS 'Moeda ISO-4217 (ex.: BRL)';

-- Seed organização padrão (ligada à LOJA-01 na V147)
INSERT INTO organizations (
    id, code, legal_name, trade_name, document,
    email, phone, default_timezone, currency, status, active,
    created_at, updated_at, version
) VALUES (
    'b1000000-0000-4000-8000-000000000001',
    'ORG-DEFAULT',
    'SystemCommerce Organização Padrão LTDA',
    'SystemCommerce',
    NULL,
    'contato@systemcommerce.local',
    NULL,
    'America/Sao_Paulo',
    'BRL',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;
