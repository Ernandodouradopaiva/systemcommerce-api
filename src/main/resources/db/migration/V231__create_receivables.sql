-- V231: Contas a receber + parcelas + origem + rateio + histórico (Prompts 99–100)
CREATE TABLE receivables (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NULL,
    customer_id             UUID            NOT NULL,
    salesperson_id          UUID            NULL,
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
    received_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    balance_amount          NUMERIC(18, 2)  NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    notes                   VARCHAR(2000)   NULL,
    cancel_reason           VARCHAR(500)    NULL,
    write_off_reason        VARCHAR(500)    NULL,
    idempotency_key         VARCHAR(100)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_receivables PRIMARY KEY (id),
    CONSTRAINT fk_recv_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_recv_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_recv_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_recv_condition FOREIGN KEY (payment_condition_id) REFERENCES payment_conditions (id),
    CONSTRAINT fk_recv_category FOREIGN KEY (financial_category_id) REFERENCES financial_categories (id),
    CONSTRAINT fk_recv_cost_center FOREIGN KEY (cost_center_id) REFERENCES cost_centers (id),
    CONSTRAINT uk_recv_org_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT ck_recv_status CHECK (status IN (
        'DRAFT', 'OPEN', 'PARTIALLY_RECEIVED', 'RECEIVED', 'OVERDUE',
        'CANCELLED', 'RENEGOTIATED', 'WRITTEN_OFF'
    )),
    CONSTRAINT ck_recv_amounts CHECK (
        original_amount >= 0 AND total_amount >= 0 AND received_amount >= 0 AND balance_amount >= 0
    )
);

CREATE INDEX idx_recv_org_status ON receivables (organization_id, status);
CREATE INDEX idx_recv_customer ON receivables (customer_id);

CREATE TABLE receivable_origins (
    id                  UUID            NOT NULL,
    receivable_id       UUID            NOT NULL,
    origin_type         VARCHAR(40)     NOT NULL,
    origin_document_id  UUID            NOT NULL,
    origin_document_number VARCHAR(60)  NULL,
    CONSTRAINT pk_receivable_origins PRIMARY KEY (id),
    CONSTRAINT uk_ro_doc UNIQUE (origin_type, origin_document_id),
    CONSTRAINT fk_ro_recv FOREIGN KEY (receivable_id) REFERENCES receivables (id) ON DELETE CASCADE,
    CONSTRAINT ck_ro_type CHECK (origin_type IN (
        'SALES_ORDER', 'SALE', 'POS', 'MARKETPLACE', 'SERVICE',
        'MANUAL_CHARGE', 'ADVANCE', 'RENEGOTIATION', 'ADJUSTMENT'
    ))
);

CREATE TABLE receivable_installments (
    id                  UUID            NOT NULL,
    receivable_id       UUID            NOT NULL,
    installment_number  INT             NOT NULL,
    issue_date          DATE            NOT NULL,
    due_date            DATE            NOT NULL,
    original_amount     NUMERIC(18, 2)  NOT NULL,
    interest_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fine_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    received_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    balance_amount      NUMERIC(18, 2)  NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
    nosso_numero        VARCHAR(40)     NULL,
    billing_code        VARCHAR(80)     NULL,
    pix_txid            VARCHAR(80)     NULL,
    boleto_number       VARCHAR(80)     NULL,
    notes               VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_receivable_installments PRIMARY KEY (id),
    CONSTRAINT uk_ri_recv_number UNIQUE (receivable_id, installment_number),
    CONSTRAINT fk_ri_recv FOREIGN KEY (receivable_id) REFERENCES receivables (id) ON DELETE CASCADE,
    CONSTRAINT ck_ri_status CHECK (status IN (
        'OPEN', 'PARTIALLY_RECEIVED', 'RECEIVED', 'OVERDUE', 'CANCELLED', 'RENEGOTIATED', 'WRITTEN_OFF'
    )),
    CONSTRAINT ck_ri_amounts CHECK (
        original_amount >= 0 AND received_amount >= 0 AND balance_amount >= 0
    )
);

CREATE INDEX idx_ri_due ON receivable_installments (due_date, status);

CREATE TABLE receivable_allocations (
    id                  UUID            NOT NULL,
    receivable_id       UUID            NOT NULL,
    cost_center_id      UUID            NOT NULL,
    percentage          NUMERIC(8, 4)   NOT NULL,
    amount              NUMERIC(18, 2)  NOT NULL,
    CONSTRAINT pk_receivable_allocations PRIMARY KEY (id),
    CONSTRAINT fk_ra_recv FOREIGN KEY (receivable_id) REFERENCES receivables (id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_cc FOREIGN KEY (cost_center_id) REFERENCES cost_centers (id)
);

CREATE TABLE receivable_status_history (
    id                  UUID            NOT NULL,
    receivable_id       UUID            NOT NULL,
    from_status         VARCHAR(30)     NULL,
    to_status           VARCHAR(30)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    CONSTRAINT pk_rsh PRIMARY KEY (id),
    CONSTRAINT fk_rsh_recv FOREIGN KEY (receivable_id) REFERENCES receivables (id) ON DELETE CASCADE
);

COMMENT ON TABLE receivables IS 'Contas a receber (Prompt 99) — não altera estoque';
