-- V240: Renegociação financeira (Prompt 110)
CREATE TABLE financial_renegotiations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    document_side VARCHAR(20) NOT NULL,
    original_document_id UUID NOT NULL,
    new_document_id UUID,
    status VARCHAR(30) NOT NULL,
    renegotiation_date DATE NOT NULL,
    balance_before NUMERIC(18,2) NOT NULL,
    interest_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    penalty_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    down_payment_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    advance_application_id UUID,
    new_total_amount NUMERIC(18,2) NOT NULL,
    payment_condition_id UUID REFERENCES payment_conditions(id),
    charge_policy_id UUID REFERENCES financial_charge_policies(id),
    authorization_required BOOLEAN NOT NULL DEFAULT FALSE,
    authorized_by UUID,
    authorized_at TIMESTAMPTZ,
    reason VARCHAR(500) NOT NULL,
    cancel_reason VARCHAR(500),
    idempotency_key VARCHAR(100),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_fin_reneg_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE financial_renegotiation_items (
    id UUID PRIMARY KEY,
    renegotiation_id UUID NOT NULL REFERENCES financial_renegotiations(id) ON DELETE CASCADE,
    original_installment_id UUID NOT NULL,
    original_balance NUMERIC(18,2) NOT NULL
);

CREATE TABLE financial_renegotiation_installments (
    id UUID PRIMARY KEY,
    renegotiation_id UUID NOT NULL REFERENCES financial_renegotiations(id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    generated_installment_id UUID
);

CREATE TABLE financial_renegotiation_status_history (
    id UUID PRIMARY KEY,
    renegotiation_id UUID NOT NULL REFERENCES financial_renegotiations(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by UUID,
    notes VARCHAR(1000)
);

CREATE INDEX idx_fin_reneg_org ON financial_renegotiations(organization_id);
CREATE INDEX idx_fin_reneg_doc ON financial_renegotiations(document_side, original_document_id);
