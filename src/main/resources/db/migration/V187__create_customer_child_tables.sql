-- V187: tabelas filhas do cadastro de clientes (Prompt 58)
-- Endereços/contatos múltiplos, condições comerciais, histórico de status e consentimentos (LGPD).
-- Cliente permanece GLOBAL na organização; nada aqui duplica cadastro por loja.

CREATE TABLE customer_addresses (
    id                  UUID            NOT NULL,
    customer_id         UUID            NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    zip_code            VARCHAR(10)     NULL,
    street              VARCHAR(200)    NULL,
    number              VARCHAR(20)     NULL,
    complement          VARCHAR(100)    NULL,
    district            VARCHAR(100)    NULL,
    city                VARCHAR(100)    NULL,
    state               VARCHAR(2)      NULL,
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    notes               VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_customer_addresses PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_customer_addresses_type CHECK (type IN ('COMMERCIAL', 'BILLING', 'DELIVERY', 'OTHER')),
    CONSTRAINT ck_customer_addresses_state_format CHECK (state IS NULL OR state ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_customer_addresses_customer ON customer_addresses (customer_id, active);
CREATE INDEX idx_customer_addresses_type ON customer_addresses (customer_id, type);

COMMENT ON TABLE customer_addresses IS 'Endereços múltiplos do cliente (comercial/cobrança/entrega/outro)';

CREATE TABLE customer_contacts (
    id                  UUID            NOT NULL,
    customer_id         UUID            NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    name                VARCHAR(150)    NULL,
    email               VARCHAR(255)    NULL,
    phone               VARCHAR(30)     NULL,
    mobile              VARCHAR(30)     NULL,
    role_description    VARCHAR(100)    NULL,
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    notes               VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_customer_contacts PRIMARY KEY (id),
    CONSTRAINT fk_customer_contacts_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_customer_contacts_type CHECK (type IN ('GENERAL', 'FINANCIAL', 'COMMERCIAL', 'OTHER'))
);

CREATE INDEX idx_customer_contacts_customer ON customer_contacts (customer_id, active);
CREATE INDEX idx_customer_contacts_type ON customer_contacts (customer_id, type);

COMMENT ON TABLE customer_contacts IS 'Contatos múltiplos do cliente (geral/financeiro/comercial/outro)';

CREATE TABLE customer_commercial_conditions (
    id                      UUID            NOT NULL,
    customer_id             UUID            NOT NULL,
    payment_term_days       INTEGER         NULL,
    payment_condition       VARCHAR(100)    NULL,
    price_table_id          UUID            NULL,
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_customer_commercial_conditions PRIMARY KEY (id),
    CONSTRAINT fk_customer_commercial_conditions_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_customer_commercial_conditions_price_table FOREIGN KEY (price_table_id) REFERENCES price_tables (id),
    CONSTRAINT uk_customer_commercial_conditions_customer UNIQUE (customer_id),
    CONSTRAINT ck_customer_commercial_conditions_term CHECK (payment_term_days IS NULL OR payment_term_days >= 0)
);

CREATE INDEX idx_customer_commercial_conditions_customer ON customer_commercial_conditions (customer_id);

COMMENT ON TABLE customer_commercial_conditions IS
    'Condição comercial em nível de organização (prazo, condição de pagamento, tabela de preço sugerida)';

CREATE TABLE customer_status_history (
    id                  UUID            NOT NULL,
    customer_id         UUID            NOT NULL,
    previous_status     VARCHAR(20)     NULL,
    new_status          VARCHAR(20)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    changed_by          UUID            NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_customer_status_history PRIMARY KEY (id),
    CONSTRAINT fk_customer_status_history_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_customer_status_history_user FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT ck_customer_status_history_new_status CHECK (new_status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_customer_status_history_customer ON customer_status_history (customer_id, changed_at DESC);

COMMENT ON TABLE customer_status_history IS 'Histórico imutável de mudanças de status do cliente — nunca apagado';

CREATE TABLE customer_consents (
    id                  UUID            NOT NULL,
    customer_id         UUID            NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    granted             BOOLEAN         NOT NULL,
    granted_at          TIMESTAMPTZ     NULL,
    revoked_at          TIMESTAMPTZ     NULL,
    notes               VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_customer_consents PRIMARY KEY (id),
    CONSTRAINT fk_customer_consents_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_customer_consents_type CHECK (type IN ('MARKETING', 'DATA_PROCESSING', 'OTHER'))
);

CREATE INDEX idx_customer_consents_customer ON customer_consents (customer_id, active);

COMMENT ON TABLE customer_consents IS 'Consentimentos LGPD do cliente (marketing/tratamento de dados/outro) — histórico não apagável';

-- Vínculo local cliente-loja: possibilidade de sobrescrever limite de crédito por loja
ALTER TABLE customer_store_relationships
    ADD COLUMN IF NOT EXISTS credit_limit_override NUMERIC(18, 2) NULL;

COMMENT ON COLUMN customer_store_relationships.credit_limit_override IS
    'Limite de crédito específico da loja — quando nulo, usa o limite global do cliente';

-- Snapshot histórico: alteração futura do cadastro do cliente não deve alterar documentos já emitidos.
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS customer_name_snapshot VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS customer_document_snapshot VARCHAR(20) NULL;

ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS customer_name_snapshot VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS customer_document_snapshot VARCHAR(20) NULL;

COMMENT ON COLUMN sales.customer_name_snapshot IS
    'Nome do cliente no momento da associação à venda — não é recalculado em edições futuras do cadastro';
COMMENT ON COLUMN sales.customer_document_snapshot IS
    'CPF/CNPJ do cliente no momento da associação à venda (snapshot histórico)';
COMMENT ON COLUMN sales_orders.customer_name_snapshot IS
    'Nome do cliente no momento da associação ao pedido — não é recalculado em edições futuras do cadastro';
COMMENT ON COLUMN sales_orders.customer_document_snapshot IS
    'CPF/CNPJ do cliente no momento da associação ao pedido (snapshot histórico)';
