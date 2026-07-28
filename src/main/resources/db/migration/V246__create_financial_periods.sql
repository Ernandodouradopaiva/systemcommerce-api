-- V246: Fechamento financeiro por período (Prompt 115)
CREATE TABLE financial_periods (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    timezone VARCHAR(60) NOT NULL DEFAULT 'America/Sao_Paulo',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_financial_periods_org_code UNIQUE (organization_id, code),
    CONSTRAINT ck_financial_periods_dates CHECK (end_date >= start_date)
);

CREATE TABLE financial_closings (
    id UUID PRIMARY KEY,
    period_id UUID NOT NULL REFERENCES financial_periods(id),
    closed_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    closed_by UUID,
    notes VARCHAR(2000),
    blockers_count INTEGER NOT NULL DEFAULT 0,
    warnings_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE financial_closing_checks (
    id UUID PRIMARY KEY,
    closing_id UUID NOT NULL REFERENCES financial_closings(id) ON DELETE CASCADE,
    check_code VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    passed BOOLEAN NOT NULL,
    message VARCHAR(1000) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE financial_closing_reopenings (
    id UUID PRIMARY KEY,
    closing_id UUID NOT NULL REFERENCES financial_closings(id),
    reason VARCHAR(2000) NOT NULL,
    reopened_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    reopened_by UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE financial_closing_balance_snapshots (
    id UUID PRIMARY KEY,
    closing_id UUID NOT NULL REFERENCES financial_closings(id) ON DELETE CASCADE,
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    balance_amount NUMERIC(18,2) NOT NULL,
    holder_code VARCHAR(40),
    holder_name VARCHAR(200)
);

CREATE INDEX idx_financial_periods_org_status ON financial_periods(organization_id, status);
CREATE INDEX idx_financial_periods_dates ON financial_periods(organization_id, start_date, end_date);
CREATE INDEX idx_financial_closings_period ON financial_closings(period_id);
