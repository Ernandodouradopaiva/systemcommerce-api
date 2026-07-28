-- V25: ampliação do módulo de pagamentos (formas, campos financeiros, histórico)

ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payments_method;
ALTER TABLE payments
    ADD CONSTRAINT ck_payments_method CHECK (
        method IN (
            'CASH',
            'PIX',
            'DEBIT_CARD',
            'CREDIT_CARD',
            'TRANSFER',
            'BANK_SLIP',
            'OTHER'
        )
    );

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS external_reference VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS installments INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS tendered_amount NUMERIC(19, 2) NULL,
    ADD COLUMN IF NOT EXISTS responsible_user_id UUID NULL;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS fk_payments_responsible_user;
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_responsible_user
        FOREIGN KEY (responsible_user_id) REFERENCES users (id);

ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payments_installments_positive;
ALTER TABLE payments
    ADD CONSTRAINT ck_payments_installments_positive CHECK (installments >= 1);

ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payments_tendered_non_negative;
ALTER TABLE payments
    ADD CONSTRAINT ck_payments_tendered_non_negative CHECK (
        tendered_amount IS NULL OR tendered_amount >= 0
    );

CREATE INDEX IF NOT EXISTS idx_payments_responsible_user_id ON payments (responsible_user_id);
CREATE INDEX IF NOT EXISTS idx_payments_external_reference ON payments (external_reference);

CREATE TABLE IF NOT EXISTS payment_status_history (
    id                  UUID            NOT NULL,
    payment_id          UUID            NOT NULL,
    from_status         VARCHAR(20)     NULL,
    to_status           VARCHAR(20)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    CONSTRAINT pk_payment_status_history PRIMARY KEY (id),
    CONSTRAINT fk_payment_status_history_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_status_history_user FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT ck_payment_status_history_from CHECK (
        from_status IS NULL
        OR from_status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'REFUNDED')
    ),
    CONSTRAINT ck_payment_status_history_to CHECK (
        to_status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'REFUNDED')
    )
);

CREATE INDEX IF NOT EXISTS idx_payment_status_history_payment_id ON payment_status_history (payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_status_history_changed_at ON payment_status_history (changed_at);

COMMENT ON TABLE payment_status_history IS 'Trilha imutável de mudanças de status do pagamento';
COMMENT ON COLUMN payments.external_reference IS 'Referência externa (NSU, TXID PIX, etc.)';
COMMENT ON COLUMN payments.tendered_amount IS 'Valor recebido (ex.: dinheiro) para cálculo de troco na API';
COMMENT ON COLUMN payments.installments IS 'Quantidade de parcelas (>= 1)';
