-- V242: Conciliação bancária (Prompt 111)
CREATE TABLE bank_statements (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    statement_date DATE NOT NULL,
    period_start DATE,
    period_end DATE,
    opening_balance NUMERIC(18,2),
    closing_balance NUMERIC(18,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    source_type VARCHAR(40) NOT NULL,
    external_file_hash VARCHAR(128),
    original_payload TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    notes VARCHAR(2000),
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_bank_statements_org_idem UNIQUE (organization_id, idempotency_key),
    CONSTRAINT uk_bank_statements_holder_hash UNIQUE (holder_id, external_file_hash)
);

CREATE TABLE bank_statement_imports (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    statement_id UUID REFERENCES bank_statements(id),
    import_format VARCHAR(20) NOT NULL,
    file_name VARCHAR(255),
    file_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    entries_imported INT NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    original_payload TEXT,
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_bank_statement_imports_hash UNIQUE (holder_id, file_hash),
    CONSTRAINT uk_bank_statement_imports_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE bank_statement_entries (
    id UUID PRIMARY KEY,
    statement_id UUID NOT NULL REFERENCES bank_statements(id) ON DELETE CASCADE,
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    entry_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    document_number VARCHAR(80),
    amount NUMERIC(18,2) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    external_id VARCHAR(120),
    informed_balance NUMERIC(18,2),
    reconciliation_status VARCHAR(30) NOT NULL DEFAULT 'UNMATCHED',
    fit_id VARCHAR(120),
    raw_line TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_bank_statement_entries_ext UNIQUE (statement_id, external_id)
);

CREATE TABLE bank_reconciliation_rules (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    holder_id UUID REFERENCES financial_account_holders(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    match_by_amount BOOLEAN NOT NULL DEFAULT TRUE,
    match_by_date BOOLEAN NOT NULL DEFAULT TRUE,
    date_tolerance_days INT NOT NULL DEFAULT 2,
    match_by_document BOOLEAN NOT NULL DEFAULT FALSE,
    description_contains VARCHAR(200),
    auto_confirm BOOLEAN NOT NULL DEFAULT FALSE,
    safe_auto BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_bank_recon_rules_org_code UNIQUE (organization_id, code)
);

CREATE TABLE bank_reconciliations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    statement_id UUID REFERENCES bank_statements(id),
    reconciliation_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    notes VARCHAR(2000),
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_bank_reconciliations_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE bank_reconciliation_matches (
    id UUID PRIMARY KEY,
    reconciliation_id UUID NOT NULL REFERENCES bank_reconciliations(id) ON DELETE CASCADE,
    statement_entry_id UUID NOT NULL REFERENCES bank_statement_entries(id),
    holder_movement_id UUID REFERENCES financial_holder_movements(id),
    rule_id UUID REFERENCES bank_reconciliation_rules(id),
    match_status VARCHAR(30) NOT NULL,
    matched_amount NUMERIC(18,2) NOT NULL,
    divergence_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    suggested BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMPTZ,
    confirmed_by UUID,
    undone_at TIMESTAMPTZ,
    undone_by UUID,
    notes VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_bank_statement_entries_status ON bank_statement_entries(reconciliation_status);
CREATE INDEX idx_bank_statement_entries_holder_date ON bank_statement_entries(holder_id, entry_date);
CREATE INDEX idx_bank_recon_matches_entry ON bank_reconciliation_matches(statement_entry_id);
