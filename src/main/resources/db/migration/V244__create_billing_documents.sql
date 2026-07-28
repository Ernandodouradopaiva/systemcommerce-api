-- V244: Boletos e cobranças PIX (Prompt 113)
CREATE TABLE billing_documents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    store_id UUID REFERENCES stores(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    receivable_id UUID REFERENCES receivables(id),
    receivable_installment_id UUID REFERENCES receivable_installments(id),
    billing_type VARCHAR(20) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    external_id VARCHAR(120),
    provider_code VARCHAR(60),
    idempotency_key VARCHAR(100),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_billing_documents_org_idem UNIQUE (organization_id, idempotency_key),
    CONSTRAINT uk_billing_documents_ext UNIQUE (organization_id, provider_code, external_id)
);

CREATE TABLE bank_slips (
    id UUID PRIMARY KEY,
    billing_document_id UUID NOT NULL UNIQUE REFERENCES billing_documents(id) ON DELETE CASCADE,
    digitable_line VARCHAR(80),
    barcode VARCHAR(80),
    nosso_numero VARCHAR(40),
    bank_code VARCHAR(10),
    wallet VARCHAR(20),
    registered_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    paid_amount NUMERIC(18,2),
    pdf_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE pix_charges (
    id UUID PRIMARY KEY,
    billing_document_id UUID NOT NULL UNIQUE REFERENCES billing_documents(id) ON DELETE CASCADE,
    txid VARCHAR(50),
    end_to_end_id VARCHAR(50),
    qr_code TEXT,
    qr_code_image_url VARCHAR(500),
    copy_paste TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    paid_amount NUMERIC(18,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE billing_status_history (
    id UUID PRIMARY KEY,
    billing_document_id UUID NOT NULL REFERENCES billing_documents(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by UUID,
    notes VARCHAR(1000),
    external_event_id VARCHAR(120)
);

CREATE TABLE billing_webhook_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    provider_code VARCHAR(60) NOT NULL,
    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80),
    billing_document_id UUID REFERENCES billing_documents(id),
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uk_billing_webhook_events UNIQUE (organization_id, provider_code, event_id)
);

CREATE INDEX idx_billing_documents_status ON billing_documents(status);
CREATE INDEX idx_billing_documents_installment ON billing_documents(receivable_installment_id);
