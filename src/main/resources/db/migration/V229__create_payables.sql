-- V229: Contas a pagar + parcelas + origem + rateio + histórico (Prompts 96–97)
CREATE TABLE finance_generation_settings (
    id                              UUID            NOT NULL,
    organization_id                 UUID            NOT NULL,
    generate_payable_on_receipt     BOOLEAN         NOT NULL DEFAULT TRUE,
    generate_receivable_on_invoice  BOOLEAN         NOT NULL DEFAULT TRUE,
    generate_and_settle_pos_cash    BOOLEAN         NOT NULL DEFAULT TRUE,
    active                          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                      UUID            NULL,
    updated_by                      UUID            NULL,
    version                         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_finance_generation_settings PRIMARY KEY (id),
    CONSTRAINT uk_fgs_org UNIQUE (organization_id),
    CONSTRAINT fk_fgs_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE payables (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NULL,
    supplier_id             UUID            NOT NULL,
    payment_condition_id    UUID            NULL,
    financial_category_id   UUID            NULL,
    cost_center_id          UUID            NULL,
    document_number         VARCHAR(60)     NULL,
    issue_date              DATE            NOT NULL,
    competence_date         DATE            NOT NULL,
    original_amount         NUMERIC(18, 2)  NOT NULL,
    planned_discount        NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    planned_addition        NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    total_amount            NUMERIC(18, 2)  NOT NULL,
    paid_amount             NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    balance_amount          NUMERIC(18, 2)  NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    notes                   VARCHAR(2000)   NULL,
    cancel_reason           VARCHAR(500)    NULL,
    idempotency_key         VARCHAR(100)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_payables PRIMARY KEY (id),
    CONSTRAINT fk_payables_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_payables_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_payables_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_payables_condition FOREIGN KEY (payment_condition_id) REFERENCES payment_conditions (id),
    CONSTRAINT fk_payables_category FOREIGN KEY (financial_category_id) REFERENCES financial_categories (id),
    CONSTRAINT fk_payables_cost_center FOREIGN KEY (cost_center_id) REFERENCES cost_centers (id),
    CONSTRAINT uk_payables_org_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT ck_payables_status CHECK (status IN (
        'DRAFT', 'OPEN', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'SCHEDULED', 'CANCELLED', 'RENEGOTIATED'
    )),
    CONSTRAINT ck_payables_amounts CHECK (
        original_amount >= 0 AND planned_discount >= 0 AND planned_addition >= 0
        AND total_amount >= 0 AND paid_amount >= 0 AND balance_amount >= 0
    )
);

CREATE INDEX idx_payables_org_status ON payables (organization_id, status);
CREATE INDEX idx_payables_supplier ON payables (supplier_id);
CREATE INDEX idx_payables_store ON payables (store_id);
CREATE INDEX idx_payables_issue ON payables (issue_date);

CREATE TABLE payable_origins (
    id                  UUID            NOT NULL,
    payable_id          UUID            NOT NULL,
    origin_type         VARCHAR(40)     NOT NULL,
    origin_document_id  UUID            NOT NULL,
    origin_document_number VARCHAR(60)  NULL,
    CONSTRAINT pk_payable_origins PRIMARY KEY (id),
    CONSTRAINT uk_payable_origins_doc UNIQUE (origin_type, origin_document_id),
    CONSTRAINT fk_po_payable FOREIGN KEY (payable_id) REFERENCES payables (id) ON DELETE CASCADE,
    CONSTRAINT ck_po_type CHECK (origin_type IN (
        'PURCHASE_ORDER', 'PURCHASE_RECEIPT', 'SUPPLIER_INVOICE', 'FREIGHT',
        'MANUAL_EXPENSE', 'SUPPLIER_RETURN', 'ADJUSTMENT', 'ADVANCE', 'BANK_IMPORT'
    ))
);

CREATE TABLE payable_installments (
    id                  UUID            NOT NULL,
    payable_id          UUID            NOT NULL,
    installment_number  INT             NOT NULL,
    issue_date          DATE            NOT NULL,
    due_date            DATE            NOT NULL,
    original_amount     NUMERIC(18, 2)  NOT NULL,
    interest_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fine_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    settled_amount      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    balance_amount      NUMERIC(18, 2)  NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
    barcode             VARCHAR(80)     NULL,
    digitable_line      VARCHAR(80)     NULL,
    reference_code      VARCHAR(80)     NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_payable_installments PRIMARY KEY (id),
    CONSTRAINT uk_pi_payable_number UNIQUE (payable_id, installment_number),
    CONSTRAINT fk_pi_payable FOREIGN KEY (payable_id) REFERENCES payables (id) ON DELETE CASCADE,
    CONSTRAINT ck_pi_status CHECK (status IN (
        'OPEN', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'SCHEDULED', 'CANCELLED', 'RENEGOTIATED'
    )),
    CONSTRAINT ck_pi_amounts CHECK (
        original_amount >= 0 AND interest_amount >= 0 AND fine_amount >= 0
        AND discount_amount >= 0 AND settled_amount >= 0 AND balance_amount >= 0
    )
);

CREATE INDEX idx_pi_due ON payable_installments (due_date, status);
CREATE INDEX idx_pi_payable ON payable_installments (payable_id);

CREATE TABLE payable_allocations (
    id                  UUID            NOT NULL,
    payable_id          UUID            NOT NULL,
    cost_center_id      UUID            NOT NULL,
    percentage          NUMERIC(8, 4)   NOT NULL,
    amount              NUMERIC(18, 2)  NOT NULL,
    CONSTRAINT pk_payable_allocations PRIMARY KEY (id),
    CONSTRAINT fk_pa_payable FOREIGN KEY (payable_id) REFERENCES payables (id) ON DELETE CASCADE,
    CONSTRAINT fk_pa_cc FOREIGN KEY (cost_center_id) REFERENCES cost_centers (id),
    CONSTRAINT ck_pa_pct CHECK (percentage > 0 AND percentage <= 100)
);

CREATE TABLE payable_status_history (
    id                  UUID            NOT NULL,
    payable_id          UUID            NOT NULL,
    from_status         VARCHAR(30)     NULL,
    to_status           VARCHAR(30)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    CONSTRAINT pk_payable_status_history PRIMARY KEY (id),
    CONSTRAINT fk_psh_payable FOREIGN KEY (payable_id) REFERENCES payables (id) ON DELETE CASCADE
);

COMMENT ON TABLE payables IS 'Contas a pagar (Prompt 96) — não altera estoque';
COMMENT ON TABLE payable_origins IS 'Origem única por documento (anti-duplicidade)';
