-- V7: clientes
CREATE TABLE customers (
    id              UUID            NOT NULL,
    type            VARCHAR(2)      NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    document        VARCHAR(20)     NOT NULL,
    email           VARCHAR(255)    NULL,
    phone           VARCHAR(30)     NULL,
    zip_code        VARCHAR(10)     NULL,
    street          VARCHAR(200)    NULL,
    number          VARCHAR(20)     NULL,
    complement      VARCHAR(100)    NULL,
    district        VARCHAR(100)    NULL,
    city            VARCHAR(100)    NULL,
    state           VARCHAR(2)      NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_document UNIQUE (document),
    CONSTRAINT ck_customers_type CHECK (type IN ('PF', 'PJ')),
    CONSTRAINT ck_customers_name_not_blank CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_customers_document_not_blank CHECK (LENGTH(TRIM(document)) > 0),
    CONSTRAINT ck_customers_state_format CHECK (state IS NULL OR state ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_customers_document ON customers (document);
CREATE INDEX idx_customers_name ON customers (name);
CREATE INDEX idx_customers_email ON customers (email);
CREATE INDEX idx_customers_active ON customers (active);
CREATE INDEX idx_customers_type ON customers (type);

COMMENT ON TABLE customers IS 'Clientes pessoa física (PF) ou jurídica (PJ)';
COMMENT ON COLUMN customers.document IS 'CPF ou CNPJ somente dígitos';
