-- V239: Estornos / reversões financeiras (Prompt 109)
CREATE TABLE financial_reversals (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    source_type VARCHAR(40) NOT NULL,
    source_document_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    authorized_by UUID,
    authorized_at TIMESTAMPTZ,
    partial BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key VARCHAR(100),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_fin_reversals_org_idem UNIQUE (organization_id, idempotency_key),
    CONSTRAINT uk_fin_reversals_source UNIQUE (source_type, source_document_id)
);

CREATE TABLE financial_reversal_items (
    id UUID PRIMARY KEY,
    reversal_id UUID NOT NULL REFERENCES financial_reversals(id) ON DELETE CASCADE,
    item_type VARCHAR(40) NOT NULL,
    original_movement_id UUID REFERENCES financial_holder_movements(id),
    reversal_movement_id UUID REFERENCES financial_holder_movements(id),
    original_amount NUMERIC(18,2) NOT NULL,
    reversed_amount NUMERIC(18,2) NOT NULL,
    target_installment_id UUID,
    notes VARCHAR(1000)
);

CREATE TABLE financial_reversal_status_history (
    id UUID PRIMARY KEY,
    reversal_id UUID NOT NULL REFERENCES financial_reversals(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by UUID,
    notes VARCHAR(1000)
);

CREATE INDEX idx_fin_reversals_org ON financial_reversals(organization_id);
