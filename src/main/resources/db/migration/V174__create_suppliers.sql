-- V174: cadastro de fornecedores (Prompt 56)
CREATE TABLE suppliers (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    code                VARCHAR(30)     NOT NULL,
    type                VARCHAR(2)      NOT NULL,
    document            VARCHAR(20)     NOT NULL,
    state_registration  VARCHAR(30)     NULL,
    legal_name          VARCHAR(200)    NOT NULL,
    trade_name          VARCHAR(200)    NULL,
    contact_name        VARCHAR(150)    NULL,
    phone               VARCHAR(30)     NULL,
    mobile              VARCHAR(30)     NULL,
    email               VARCHAR(255)    NULL,
    website             VARCHAR(255)    NULL,
    zip_code            VARCHAR(10)     NULL,
    street              VARCHAR(200)    NULL,
    number              VARCHAR(20)     NULL,
    complement          VARCHAR(100)    NULL,
    district            VARCHAR(100)    NULL,
    city                VARCHAR(100)    NULL,
    state               VARCHAR(2)      NULL,
    notes               VARCHAR(2000)   NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    registered_at       TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_suppliers PRIMARY KEY (id),
    CONSTRAINT uk_suppliers_document UNIQUE (document),
    CONSTRAINT uk_suppliers_code UNIQUE (code),
    CONSTRAINT fk_suppliers_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_suppliers_type CHECK (type IN ('PF', 'PJ')),
    CONSTRAINT ck_suppliers_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_suppliers_legal_name_not_blank CHECK (LENGTH(TRIM(legal_name)) > 0),
    CONSTRAINT ck_suppliers_document_not_blank CHECK (LENGTH(TRIM(document)) > 0),
    CONSTRAINT ck_suppliers_code_not_blank CHECK (LENGTH(TRIM(code)) > 0),
    CONSTRAINT ck_suppliers_state_format CHECK (state IS NULL OR state ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_suppliers_document ON suppliers (document);
CREATE INDEX idx_suppliers_code ON suppliers (code);
CREATE INDEX idx_suppliers_legal_name ON suppliers (legal_name);
CREATE INDEX idx_suppliers_trade_name ON suppliers (trade_name);
CREATE INDEX idx_suppliers_status ON suppliers (status);
CREATE INDEX idx_suppliers_active ON suppliers (active);
CREATE INDEX idx_suppliers_organization ON suppliers (organization_id);

COMMENT ON TABLE suppliers IS 'Fornecedores PF/PJ — cadastro comercial (Prompt 56)';
COMMENT ON COLUMN suppliers.document IS 'CPF ou CNPJ somente dígitos';
COMMENT ON COLUMN suppliers.code IS 'Código interno único';
