-- V247: DRE gerencial (Prompt 116) — relatório gerencial, não demonstração contábil oficial
CREATE TABLE income_statement_layouts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_income_statement_layouts UNIQUE (organization_id, code)
);

CREATE TABLE income_statement_lines (
    id UUID PRIMARY KEY,
    layout_id UUID NOT NULL REFERENCES income_statement_layouts(id) ON DELETE CASCADE,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    line_type VARCHAR(30) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    formula VARCHAR(500),
    formula_doc VARCHAR(1000),
    sign_multiplier INTEGER NOT NULL DEFAULT 1,
    parent_line_id UUID REFERENCES income_statement_lines(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_income_statement_lines UNIQUE (layout_id, code)
);

CREATE TABLE income_statement_mappings (
    id UUID PRIMARY KEY,
    layout_id UUID NOT NULL REFERENCES income_statement_layouts(id) ON DELETE CASCADE,
    line_id UUID NOT NULL REFERENCES income_statement_lines(id) ON DELETE CASCADE,
    financial_category_id UUID REFERENCES financial_categories(id),
    financial_account_id UUID REFERENCES financial_accounts(id),
    source_type VARCHAR(40) NOT NULL DEFAULT 'CATEGORY',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE income_statement_executions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    layout_id UUID NOT NULL REFERENCES income_statement_layouts(id),
    basis VARCHAR(20) NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    compare_from DATE,
    compare_to DATE,
    timezone VARCHAR(60) NOT NULL DEFAULT 'America/Sao_Paulo',
    executed_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    executed_by UUID,
    notes VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE income_statement_execution_lines (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES income_statement_executions(id) ON DELETE CASCADE,
    line_id UUID NOT NULL REFERENCES income_statement_lines(id),
    line_code VARCHAR(40) NOT NULL,
    line_name VARCHAR(200) NOT NULL,
    amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    compare_amount NUMERIC(18,2),
    variance_amount NUMERIC(18,2),
    formula_applied VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_income_statement_exec_org ON income_statement_executions(organization_id, period_from, period_to);
CREATE INDEX idx_income_statement_mappings_line ON income_statement_mappings(line_id);
