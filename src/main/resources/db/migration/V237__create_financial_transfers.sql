-- V237: Transferências financeiras (Prompt 107)
CREATE TABLE financial_transfers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    source_holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    target_holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    source_store_id UUID REFERENCES stores(id),
    target_store_id UUID REFERENCES stores(id),
    cash_session_id UUID REFERENCES cash_sessions(id),
    transfer_date DATE NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    fee_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    reason VARCHAR(500) NOT NULL,
    reference_code VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    source_movement_id UUID REFERENCES financial_holder_movements(id),
    target_movement_id UUID REFERENCES financial_holder_movements(id),
    fee_movement_id UUID REFERENCES financial_holder_movements(id),
    reverse_of_id UUID REFERENCES financial_transfers(id),
    idempotency_key VARCHAR(100),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_fin_transfer_diff_holders CHECK (source_holder_id <> target_holder_id),
    CONSTRAINT uk_fin_transfers_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE financial_transfer_status_history (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES financial_transfers(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by UUID,
    notes VARCHAR(1000)
);

CREATE INDEX idx_fin_transfers_org ON financial_transfers(organization_id);
CREATE INDEX idx_fin_transfers_status ON financial_transfers(status);
