-- V248: Cenários de fluxo, auditoria de exportação e índices (Prompts 114/117/118)
CREATE TABLE cash_flow_scenarios (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    inflow_factor NUMERIC(10,4) NOT NULL DEFAULT 1,
    outflow_factor NUMERIC(10,4) NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_cash_flow_scenarios UNIQUE (organization_id, code)
);

CREATE TABLE finance_report_export_audits (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    user_id UUID,
    report_type VARCHAR(60) NOT NULL,
    export_format VARCHAR(10) NOT NULL,
    filters_json TEXT,
    row_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

-- Índices para consultas de fluxo/dashboard/relatórios
CREATE INDEX IF NOT EXISTS idx_holder_movements_occurred
    ON financial_holder_movements(holder_id, occurred_at)
    WHERE reversed = FALSE AND active = TRUE;

CREATE INDEX IF NOT EXISTS idx_receivable_installments_due_status
    ON receivable_installments(due_date, status);

CREATE INDEX IF NOT EXISTS idx_payable_installments_due_status
    ON payable_installments(due_date, status);

CREATE INDEX IF NOT EXISTS idx_card_schedules_expected_status
    ON card_receivable_schedules(expected_date, status);

CREATE INDEX IF NOT EXISTS idx_billing_documents_due_status
    ON billing_documents(due_date, status);

CREATE INDEX IF NOT EXISTS idx_receivable_settlements_payment_date
    ON receivable_settlements(organization_id, payment_date, status);

CREATE INDEX IF NOT EXISTS idx_payable_settlements_payment_date
    ON payable_settlements(organization_id, payment_date, status);

CREATE INDEX IF NOT EXISTS idx_finance_report_exports_org
    ON finance_report_export_audits(organization_id, created_at);
