-- V236: Políticas financeiras (Prompt 106)
CREATE TABLE financial_charge_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    priority INT NOT NULL DEFAULT 100,
    valid_from DATE NOT NULL,
    valid_to DATE,
    interest_type VARCHAR(40) NOT NULL DEFAULT 'NONE',
    interest_rate NUMERIC(18,6) NOT NULL DEFAULT 0,
    interest_grace_days INT NOT NULL DEFAULT 0,
    penalty_type VARCHAR(40) NOT NULL DEFAULT 'NONE',
    penalty_fixed_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    penalty_percent NUMERIC(18,6) NOT NULL DEFAULT 0,
    early_discount_type VARCHAR(40) NOT NULL DEFAULT 'NONE',
    early_discount_percent NUMERIC(18,6) NOT NULL DEFAULT 0,
    early_discount_days INT NOT NULL DEFAULT 0,
    max_authorized_discount_percent NUMERIC(18,6) NOT NULL DEFAULT 0,
    requires_discount_authorization BOOLEAN NOT NULL DEFAULT FALSE,
    rounding_mode VARCHAR(20) NOT NULL DEFAULT 'HALF_UP',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_fin_charge_policies_org_code UNIQUE (organization_id, code)
);

CREATE TABLE settlement_adjustments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    settlement_type VARCHAR(40) NOT NULL,
    settlement_id UUID NOT NULL,
    installment_id UUID,
    adjustment_type VARCHAR(40) NOT NULL,
    policy_id UUID REFERENCES financial_charge_policies(id),
    base_date DATE NOT NULL,
    calculated_amount NUMERIC(18,2) NOT NULL,
    applied_amount NUMERIC(18,2) NOT NULL,
    authorized BOOLEAN NOT NULL DEFAULT FALSE,
    authorized_by UUID,
    notes VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_fin_charge_policies_org ON financial_charge_policies(organization_id);
CREATE INDEX idx_settlement_adjustments_settlement ON settlement_adjustments(settlement_type, settlement_id);
