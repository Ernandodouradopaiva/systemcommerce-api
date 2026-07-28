-- V250: Aprovação financeira em duas etapas + política (Prompt 119)
CREATE TABLE financial_approval_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    require_payment_approval BOOLEAN NOT NULL DEFAULT FALSE,
    payment_approval_threshold NUMERIC(18,2) NOT NULL DEFAULT 0,
    require_reversal_approval BOOLEAN NOT NULL DEFAULT TRUE,
    require_discount_approval BOOLEAN NOT NULL DEFAULT TRUE,
    discount_approval_threshold NUMERIC(18,2) NOT NULL DEFAULT 0,
    require_transfer_approval BOOLEAN NOT NULL DEFAULT FALSE,
    transfer_approval_threshold NUMERIC(18,2) NOT NULL DEFAULT 0,
    require_period_reopen_approval BOOLEAN NOT NULL DEFAULT TRUE,
    require_manual_entry_approval BOOLEAN NOT NULL DEFAULT FALSE,
    manual_entry_approval_threshold NUMERIC(18,2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_financial_approval_policies_org UNIQUE (organization_id)
);

CREATE TABLE financial_approval_requests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    operation_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    source_entity_type VARCHAR(60) NOT NULL,
    source_entity_id UUID,
    amount NUMERIC(18,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    payload_json TEXT,
    reason VARCHAR(2000),
    decision_notes VARCHAR(2000),
    requested_by UUID,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    decided_by UUID,
    decided_at TIMESTAMPTZ,
    executed_at TIMESTAMPTZ,
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_financial_approval_requests_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE finance_migration_runs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    dry_run BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL,
    sales_scanned INTEGER NOT NULL DEFAULT 0,
    receivables_created INTEGER NOT NULL DEFAULT 0,
    purchases_scanned INTEGER NOT NULL DEFAULT 0,
    payables_created INTEGER NOT NULL DEFAULT 0,
    skipped_duplicates INTEGER NOT NULL DEFAULT 0,
    errors_count INTEGER NOT NULL DEFAULT 0,
    report_json TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    finished_at TIMESTAMPTZ,
    started_by UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_financial_approval_requests_status
    ON financial_approval_requests(organization_id, status, operation_type);
CREATE INDEX idx_finance_migration_runs_org
    ON finance_migration_runs(organization_id, started_at DESC);
