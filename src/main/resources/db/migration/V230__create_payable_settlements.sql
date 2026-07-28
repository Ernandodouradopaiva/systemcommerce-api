-- V230: Liquidação de contas a pagar (Prompt 98)
CREATE TABLE payable_settlements (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NULL,
    holder_id               UUID            NOT NULL,
    payment_method_id       UUID            NULL,
    payment_date            DATE            NOT NULL,
    effective_date          DATE            NOT NULL,
    principal_amount        NUMERIC(18, 2)  NOT NULL,
    interest_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fine_amount             NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fee_amount              NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    total_disbursed         NUMERIC(18, 2)  NOT NULL,
    reference_code          VARCHAR(120)    NULL,
    receipt_url             VARCHAR(500)    NULL,
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
    CONSTRAINT pk_payable_settlements PRIMARY KEY (id),
    CONSTRAINT uk_ps_org_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT fk_ps_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ps_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_ps_holder FOREIGN KEY (holder_id) REFERENCES financial_account_holders (id),
    CONSTRAINT fk_ps_method FOREIGN KEY (payment_method_id) REFERENCES fin_payment_methods (id),
    CONSTRAINT fk_ps_movement FOREIGN KEY (holder_movement_id) REFERENCES financial_holder_movements (id),
    CONSTRAINT ck_ps_status CHECK (status IN (
        'PENDING', 'SCHEDULED', 'CONFIRMED', 'REVERSED', 'CANCELLED', 'FAILED'
    )),
    CONSTRAINT ck_ps_amounts CHECK (
        principal_amount >= 0 AND interest_amount >= 0 AND fine_amount >= 0
        AND discount_amount >= 0 AND fee_amount >= 0 AND total_disbursed >= 0
    )
);

CREATE TABLE payable_settlement_allocations (
    id                      UUID            NOT NULL,
    settlement_id           UUID            NOT NULL,
    installment_id          UUID            NOT NULL,
    principal_amount        NUMERIC(18, 2)  NOT NULL,
    interest_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    fine_amount             NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    allocated_total         NUMERIC(18, 2)  NOT NULL,
    CONSTRAINT pk_payable_settlement_allocations PRIMARY KEY (id),
    CONSTRAINT fk_psa_settlement FOREIGN KEY (settlement_id) REFERENCES payable_settlements (id) ON DELETE CASCADE,
    CONSTRAINT fk_psa_installment FOREIGN KEY (installment_id) REFERENCES payable_installments (id),
    CONSTRAINT ck_psa_amounts CHECK (principal_amount >= 0 AND allocated_total >= 0)
);

CREATE TABLE payable_settlement_status_history (
    id                  UUID            NOT NULL,
    settlement_id       UUID            NOT NULL,
    from_status         VARCHAR(30)     NULL,
    to_status           VARCHAR(30)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    CONSTRAINT pk_pssh PRIMARY KEY (id),
    CONSTRAINT fk_pssh_settlement FOREIGN KEY (settlement_id) REFERENCES payable_settlements (id) ON DELETE CASCADE
);

CREATE INDEX idx_ps_holder ON payable_settlements (holder_id);
CREATE INDEX idx_ps_status ON payable_settlements (organization_id, status);
