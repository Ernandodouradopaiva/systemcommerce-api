-- V14: pagamentos
CREATE TABLE payments (
    id                  UUID            NOT NULL,
    sale_id             UUID            NOT NULL,
    method              VARCHAR(20)     NOT NULL,
    amount              NUMERIC(19, 2)  NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    paid_at             TIMESTAMPTZ     NULL,
    notes               VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT ck_payments_method CHECK (method IN ('CASH', 'PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_SLIP', 'OTHER')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payments_sale_id ON payments (sale_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_method ON payments (method);
CREATE INDEX idx_payments_paid_at ON payments (paid_at);

COMMENT ON TABLE payments IS 'Pagamentos vinculados a uma venda';
