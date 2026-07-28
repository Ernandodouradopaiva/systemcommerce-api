-- V232: Liquidação de contas a receber (Prompt 101)
CREATE TABLE receivable_settlements (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NULL,
    customer_id             UUID            NOT NULL,
    holder_id               UUID            NOT NULL,
    payment_method_id       UUID            NULL,
    payment_date            DATE            NOT NULL,
    effective_date          DATE            NOT NULL,
    principal_amount        NUMERIC(18, 2)  NOT NULL,
    interest_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fine_amount             NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fee_amount              NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    net_amount              NUMERIC(18, 2)  NOT NULL,
    gross_amount            NUMERIC(18, 2)  NULL,
    acquirer_fee_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    reference_code          VARCHAR(120)    NULL,
    external_reference      VARCHAR(120)    NULL,
    cash_session_id         UUID            NULL,
    notes                   VARCHAR(1000)   NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    idempotency_key         VARCHAR(100)    NOT NULL,
    holder_movement_id      UUID            NULL,
    cancelled_reason        VARCHAR(500)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_receivable_settlements PRIMARY KEY (id),
    CONSTRAINT uk_rs_org_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT fk_rs_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_rs_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_rs_holder FOREIGN KEY (holder_id) REFERENCES financial_account_holders (id),
    CONSTRAINT fk_rs_method FOREIGN KEY (payment_method_id) REFERENCES fin_payment_methods (id),
    CONSTRAINT fk_rs_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id),
    CONSTRAINT fk_rs_movement FOREIGN KEY (holder_movement_id) REFERENCES financial_holder_movements (id),
    CONSTRAINT ck_rs_status CHECK (status IN (
        'PENDING', 'SCHEDULED', 'CONFIRMED', 'REVERSED', 'CANCELLED', 'FAILED'
    )),
    CONSTRAINT ck_rs_amounts CHECK (principal_amount >= 0 AND net_amount >= 0)
);

CREATE TABLE receivable_settlement_allocations (
    id                      UUID            NOT NULL,
    settlement_id           UUID            NOT NULL,
    installment_id          UUID            NOT NULL,
    principal_amount        NUMERIC(18, 2)  NOT NULL,
    interest_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fine_amount             NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    allocated_total         NUMERIC(18, 2)  NOT NULL,
    CONSTRAINT pk_rsa PRIMARY KEY (id),
    CONSTRAINT fk_rsa_settlement FOREIGN KEY (settlement_id) REFERENCES receivable_settlements (id) ON DELETE CASCADE,
    CONSTRAINT fk_rsa_installment FOREIGN KEY (installment_id) REFERENCES receivable_installments (id)
);

CREATE TABLE receivable_settlement_status_history (
    id                  UUID            NOT NULL,
    settlement_id       UUID            NOT NULL,
    from_status         VARCHAR(30)     NULL,
    to_status           VARCHAR(30)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    CONSTRAINT pk_rssh PRIMARY KEY (id),
    CONSTRAINT fk_rssh_settlement FOREIGN KEY (settlement_id) REFERENCES receivable_settlements (id) ON DELETE CASCADE
);

CREATE INDEX idx_rs_customer ON receivable_settlements (customer_id);
CREATE INDEX idx_rs_status ON receivable_settlements (organization_id, status);
