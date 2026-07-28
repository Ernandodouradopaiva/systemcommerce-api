-- V226: Bancos, contas bancárias, caixas financeiros e contas de pagamento (Prompt 94)
CREATE TABLE banks (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(20)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    short_name          VARCHAR(80)     NULL,
    country_code        VARCHAR(2)      NOT NULL DEFAULT 'BR',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_banks PRIMARY KEY (id),
    CONSTRAINT uk_banks_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_banks_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_banks_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Abstração FinancialAccountHolder (instrumento financeiro operacional — distinto do plano de contas)
CREATE TABLE financial_account_holders (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NULL,
    holder_type         VARCHAR(40)     NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'BRL',
    opening_balance     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    opening_balance_date DATE           NULL,
    allows_payments     BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_receipts     BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_reconciliation BOOLEAN       NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_financial_account_holders PRIMARY KEY (id),
    CONSTRAINT uk_faholder_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_faholder_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_faholder_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_faholder_type CHECK (holder_type IN (
        'CHECKING', 'SAVINGS', 'PAYMENT_ACCOUNT', 'DIGITAL_WALLET',
        'ADMIN_CASH', 'POS_CASH', 'PASS_THROUGH', 'MARKETPLACE'
    )),
    CONSTRAINT ck_faholder_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_faholder_opening CHECK (opening_balance >= 0)
);

CREATE INDEX idx_faholder_org_type ON financial_account_holders (organization_id, holder_type);
CREATE INDEX idx_faholder_store ON financial_account_holders (store_id);

CREATE TABLE bank_accounts (
    id                  UUID            NOT NULL,
    holder_id           UUID            NOT NULL,
    bank_id             UUID            NOT NULL,
    agency              VARCHAR(20)     NOT NULL,
    account_number      VARCHAR(30)     NOT NULL,
    account_digit       VARCHAR(5)      NULL,
    account_kind        VARCHAR(20)     NOT NULL DEFAULT 'CHECKING',
    holder_name         VARCHAR(200)    NOT NULL,
    holder_document     VARCHAR(20)     NULL,
    CONSTRAINT pk_bank_accounts PRIMARY KEY (id),
    CONSTRAINT uk_bank_accounts_holder UNIQUE (holder_id),
    CONSTRAINT fk_ba_holder FOREIGN KEY (holder_id) REFERENCES financial_account_holders (id) ON DELETE CASCADE,
    CONSTRAINT fk_ba_bank FOREIGN KEY (bank_id) REFERENCES banks (id),
    CONSTRAINT ck_ba_kind CHECK (account_kind IN ('CHECKING', 'SAVINGS', 'PAYMENT'))
);

CREATE TABLE financial_cashes (
    id                  UUID            NOT NULL,
    holder_id           UUID            NOT NULL,
    cash_kind           VARCHAR(20)     NOT NULL,
    pos_terminal_id     UUID            NULL,
    linked_cash_session_id UUID         NULL,
    CONSTRAINT pk_financial_cashes PRIMARY KEY (id),
    CONSTRAINT uk_financial_cashes_holder UNIQUE (holder_id),
    CONSTRAINT fk_fcash_holder FOREIGN KEY (holder_id) REFERENCES financial_account_holders (id) ON DELETE CASCADE,
    CONSTRAINT fk_fcash_terminal FOREIGN KEY (pos_terminal_id) REFERENCES pos_terminals (id),
    CONSTRAINT fk_fcash_session FOREIGN KEY (linked_cash_session_id) REFERENCES cash_sessions (id),
    CONSTRAINT ck_fcash_kind CHECK (cash_kind IN ('ADMIN', 'POS'))
);

CREATE TABLE payment_accounts (
    id                  UUID            NOT NULL,
    holder_id           UUID            NOT NULL,
    provider_code       VARCHAR(40)     NOT NULL,
    provider_name       VARCHAR(120)    NULL,
    external_account_id VARCHAR(120)    NULL,
    CONSTRAINT pk_payment_accounts PRIMARY KEY (id),
    CONSTRAINT uk_payment_accounts_holder UNIQUE (holder_id),
    CONSTRAINT fk_pa_holder FOREIGN KEY (holder_id) REFERENCES financial_account_holders (id) ON DELETE CASCADE
);

-- Movimentações do holder (saldo = abertura + soma; saldo atual NÃO é editável)
CREATE TABLE financial_holder_movements (
    id                  UUID            NOT NULL,
    holder_id           UUID            NOT NULL,
    movement_type       VARCHAR(40)     NOT NULL,
    amount              NUMERIC(18, 2)  NOT NULL,
    balance_after       NUMERIC(18, 2)  NOT NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    description         VARCHAR(500)    NULL,
    source_document_type VARCHAR(40)    NULL,
    source_document_id  UUID            NULL,
    reversed            BOOLEAN         NOT NULL DEFAULT FALSE,
    reversal_of_id      UUID            NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_financial_holder_movements PRIMARY KEY (id),
    CONSTRAINT fk_fhm_holder FOREIGN KEY (holder_id) REFERENCES financial_account_holders (id),
    CONSTRAINT fk_fhm_reversal FOREIGN KEY (reversal_of_id) REFERENCES financial_holder_movements (id),
    CONSTRAINT ck_fhm_type CHECK (movement_type IN (
        'OPENING_BALANCE', 'PAYMENT', 'RECEIPT', 'TRANSFER_IN', 'TRANSFER_OUT',
        'ADJUSTMENT', 'REVERSAL'
    ))
);

CREATE INDEX idx_fhm_holder_occurred ON financial_holder_movements (holder_id, occurred_at);

COMMENT ON TABLE financial_account_holders IS 'Instrumento financeiro operacional (Prompt 94) — distinto de financial_accounts (plano contábil)';
COMMENT ON TABLE financial_holder_movements IS 'Única fonte do saldo atual do holder; saldo inicial gera OPENING_BALANCE';
