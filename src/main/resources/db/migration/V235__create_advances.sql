-- V235: Adiantamentos (Prompt 105)
CREATE TABLE customer_advances (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    document_number VARCHAR(60),
    advance_date DATE NOT NULL,
    original_amount NUMERIC(18,2) NOT NULL,
    applied_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    refunded_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(18,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(2000),
    cancel_reason VARCHAR(500),
    holder_movement_id UUID REFERENCES financial_holder_movements(id),
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_advances_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE supplier_advances (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    document_number VARCHAR(60),
    advance_date DATE NOT NULL,
    original_amount NUMERIC(18,2) NOT NULL,
    applied_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    refunded_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(18,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(2000),
    cancel_reason VARCHAR(500),
    holder_movement_id UUID REFERENCES financial_holder_movements(id),
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_supplier_advances_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE advance_applications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    customer_advance_id UUID REFERENCES customer_advances(id),
    supplier_advance_id UUID REFERENCES supplier_advances(id),
    target_type VARCHAR(40) NOT NULL,
    target_document_id UUID NOT NULL,
    target_installment_id UUID,
    applied_amount NUMERIC(18,2) NOT NULL,
    application_date DATE NOT NULL,
    notes VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_advance_app_one_side CHECK (
        (customer_advance_id IS NOT NULL AND supplier_advance_id IS NULL)
        OR (customer_advance_id IS NULL AND supplier_advance_id IS NOT NULL)
    ),
    CONSTRAINT uk_advance_app_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE TABLE advance_refunds (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    customer_advance_id UUID REFERENCES customer_advances(id),
    supplier_advance_id UUID REFERENCES supplier_advances(id),
    holder_id UUID NOT NULL REFERENCES financial_account_holders(id),
    refund_amount NUMERIC(18,2) NOT NULL,
    refund_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    holder_movement_id UUID REFERENCES financial_holder_movements(id),
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    idempotency_key VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_advance_refund_one_side CHECK (
        (customer_advance_id IS NOT NULL AND supplier_advance_id IS NULL)
        OR (customer_advance_id IS NULL AND supplier_advance_id IS NOT NULL)
    ),
    CONSTRAINT uk_advance_refund_org_idem UNIQUE (organization_id, idempotency_key)
);

CREATE INDEX idx_customer_advances_org ON customer_advances(organization_id);
CREATE INDEX idx_supplier_advances_org ON supplier_advances(organization_id);
CREATE INDEX idx_advance_applications_target ON advance_applications(target_type, target_document_id);
