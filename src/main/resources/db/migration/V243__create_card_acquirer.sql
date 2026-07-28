-- V243: Cartões, adquirentes e previsão (Prompt 112)
CREATE TABLE acquirers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    document VARCHAR(20),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_acquirers_org_code UNIQUE (organization_id, code)
);

CREATE TABLE card_brands (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_card_brands_org_code UNIQUE (organization_id, code)
);

CREATE TABLE card_fee_plans (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    acquirer_id UUID NOT NULL REFERENCES acquirers(id),
    card_brand_id UUID REFERENCES card_brands(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    modality VARCHAR(30) NOT NULL,
    installment_from INT NOT NULL DEFAULT 1,
    installment_to INT NOT NULL DEFAULT 1,
    fee_percent NUMERIC(18,6) NOT NULL DEFAULT 0,
    fee_fixed NUMERIC(18,2) NOT NULL DEFAULT 0,
    settlement_days INT NOT NULL DEFAULT 1,
    valid_from DATE NOT NULL,
    valid_to DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_card_fee_plans_org_code UNIQUE (organization_id, code)
);

CREATE TABLE card_transactions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    sale_id UUID REFERENCES sales(id),
    payment_id UUID REFERENCES payments(id),
    terminal_id UUID,
    cash_session_id UUID REFERENCES cash_sessions(id),
    acquirer_id UUID NOT NULL REFERENCES acquirers(id),
    card_brand_id UUID REFERENCES card_brands(id),
    fee_plan_id UUID REFERENCES card_fee_plans(id),
    modality VARCHAR(30) NOT NULL,
    installments INT NOT NULL DEFAULT 1,
    gross_amount NUMERIC(18,2) NOT NULL,
    fee_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(18,2) NOT NULL,
    nsu VARCHAR(60),
    authorization_code VARCHAR(60),
    card_last_four VARCHAR(4),
    status VARCHAR(30) NOT NULL,
    authorized_at TIMESTAMPTZ,
    captured_at TIMESTAMPTZ,
    idempotency_key VARCHAR(100),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_card_transactions_org_idem UNIQUE (organization_id, idempotency_key),
    CONSTRAINT uk_card_transactions_sale_payment UNIQUE (sale_id, payment_id)
);

CREATE TABLE card_receivable_schedules (
    id UUID PRIMARY KEY,
    card_transaction_id UUID NOT NULL REFERENCES card_transactions(id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    expected_date DATE NOT NULL,
    gross_amount NUMERIC(18,2) NOT NULL,
    fee_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(18,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    settled_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_card_recv_sched_tx_inst UNIQUE (card_transaction_id, installment_number)
);

CREATE TABLE card_settlements (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    acquirer_id UUID NOT NULL REFERENCES acquirers(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    settlement_date DATE NOT NULL,
    gross_amount NUMERIC(18,2) NOT NULL,
    fee_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(18,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    holder_movement_id UUID REFERENCES financial_holder_movements(id),
    bank_statement_entry_id UUID REFERENCES bank_statement_entries(id),
    idempotency_key VARCHAR(100),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_card_settlements_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE card_settlement_items (
    id UUID PRIMARY KEY,
    settlement_id UUID NOT NULL REFERENCES card_settlements(id) ON DELETE CASCADE,
    schedule_id UUID NOT NULL REFERENCES card_receivable_schedules(id),
    amount NUMERIC(18,2) NOT NULL
);

CREATE TABLE card_chargebacks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    card_transaction_id UUID NOT NULL REFERENCES card_transactions(id),
    schedule_id UUID REFERENCES card_receivable_schedules(id),
    chargeback_date DATE NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    adjustment_entry_id UUID,
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_card_chargebacks_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE INDEX idx_card_recv_sched_expected ON card_receivable_schedules(expected_date, status);
CREATE INDEX idx_card_transactions_sale ON card_transactions(sale_id);
