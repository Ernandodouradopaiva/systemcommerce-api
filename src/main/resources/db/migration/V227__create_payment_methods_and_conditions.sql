-- V227: Formas e condições de pagamento (Prompt 95) — catálogo centralizado
CREATE TABLE fin_payment_methods (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NOT NULL,
    method_type         VARCHAR(40)     NOT NULL,
    allows_purchase     BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_sale         BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_pos          BOOLEAN         NOT NULL DEFAULT TRUE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    sort_order          INT             NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fin_payment_methods PRIMARY KEY (id),
    CONSTRAINT uk_fpm_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_fpm_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_fpm_type CHECK (method_type IN (
        'CASH', 'PIX', 'DEBIT', 'CREDIT', 'BANK_SLIP', 'TRANSFER', 'CHECK',
        'VOUCHER', 'DIGITAL_WALLET', 'CUSTOMER_CREDIT', 'OTHER'
    )),
    CONSTRAINT ck_fpm_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE payment_conditions (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NOT NULL,
    condition_type      VARCHAR(40)     NOT NULL,
    installment_count   INT             NOT NULL DEFAULT 1,
    interval_days       INT             NOT NULL DEFAULT 0,
    first_due_days      INT             NOT NULL DEFAULT 0,
    min_amount          NUMERIC(18, 2)  NULL,
    allows_purchase     BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_sale         BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_pos          BOOLEAN         NOT NULL DEFAULT TRUE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_payment_conditions PRIMARY KEY (id),
    CONSTRAINT uk_pc_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_pc_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_pc_type CHECK (condition_type IN (
        'CASH', 'NET_DAYS', 'INSTALLMENTS', 'ENTRY_PLUS_INSTALLMENTS', 'CUSTOM'
    )),
    CONSTRAINT ck_pc_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_pc_installments CHECK (installment_count >= 1),
    CONSTRAINT ck_pc_interval CHECK (interval_days >= 0),
    CONSTRAINT ck_pc_first_due CHECK (first_due_days >= 0)
);

CREATE TABLE payment_condition_installments (
    id                  UUID            NOT NULL,
    payment_condition_id UUID           NOT NULL,
    sequence_no         INT             NOT NULL,
    days_offset         INT             NOT NULL DEFAULT 0,
    percentage          NUMERIC(8, 4)   NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_payment_condition_installments PRIMARY KEY (id),
    CONSTRAINT uk_pci_condition_seq UNIQUE (payment_condition_id, sequence_no),
    CONSTRAINT fk_pci_condition FOREIGN KEY (payment_condition_id) REFERENCES payment_conditions (id) ON DELETE CASCADE,
    CONSTRAINT ck_pci_seq CHECK (sequence_no >= 1),
    CONSTRAINT ck_pci_pct CHECK (percentage > 0 AND percentage <= 100)
);

CREATE TABLE payment_method_store_configurations (
    id                  UUID            NOT NULL,
    payment_method_id   UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    allows_pos          BOOLEAN         NOT NULL DEFAULT TRUE,
    max_installments    INT             NULL,
    notes               VARCHAR(500)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_payment_method_store_configurations PRIMARY KEY (id),
    CONSTRAINT uk_pmsc_method_store UNIQUE (payment_method_id, store_id),
    CONSTRAINT fk_pmsc_method FOREIGN KEY (payment_method_id) REFERENCES fin_payment_methods (id) ON DELETE CASCADE,
    CONSTRAINT fk_pmsc_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_pmsc_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

COMMENT ON TABLE fin_payment_methods IS 'Catálogo de formas de pagamento (Prompt 95) — distinto do enum legado Payment.PaymentMethod';
COMMENT ON TABLE payment_conditions IS 'Condições de pagamento; parcelas geradas preservam snapshot no documento futuro';
