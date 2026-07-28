-- V238: Lançamentos financeiros manuais (Prompt 108)
CREATE TABLE financial_entries (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    financial_category_id UUID NOT NULL REFERENCES financial_categories(id),
    cost_center_id UUID REFERENCES cost_centers(id),
    entry_type VARCHAR(40) NOT NULL,
    entry_date DATE NOT NULL,
    competence_date DATE NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    reference_code VARCHAR(100),
    attachment_url VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    holder_movement_id UUID REFERENCES financial_holder_movements(id),
    reverse_of_id UUID REFERENCES financial_entries(id),
    cancel_reason VARCHAR(500),
    idempotency_key VARCHAR(100),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_fin_entries_positive CHECK (amount > 0),
    CONSTRAINT uk_fin_entries_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE financial_entry_status_history (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES financial_entries(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by UUID,
    notes VARCHAR(1000)
);

CREATE INDEX idx_fin_entries_org ON financial_entries(organization_id);
CREATE INDEX idx_fin_entries_status ON financial_entries(status);
