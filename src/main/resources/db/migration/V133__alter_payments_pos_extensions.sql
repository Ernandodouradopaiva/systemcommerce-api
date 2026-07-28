-- V133: extensões de pagamento para PDV (múltiplas formas, troco, TEF mínimo, idempotência)
ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payments_method;
ALTER TABLE payments
    ADD CONSTRAINT ck_payments_method CHECK (method IN (
        'CASH', 'PIX', 'DEBIT_CARD', 'CREDIT_CARD', 'TRANSFER', 'BANK_SLIP', 'VOUCHER', 'OTHER'
    ));

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS informed_amount NUMERIC(19, 2) NULL,
    ADD COLUMN IF NOT EXISTS applied_amount NUMERIC(19, 2) NULL,
    ADD COLUMN IF NOT EXISTS change_amount NUMERIC(19, 2) NULL,
    ADD COLUMN IF NOT EXISTS authorization_code VARCHAR(60) NULL,
    ADD COLUMN IF NOT EXISTS nsu VARCHAR(60) NULL,
    ADD COLUMN IF NOT EXISTS card_brand VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS acquirer VARCHAR(60) NULL,
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100) NULL;

UPDATE payments
SET informed_amount = COALESCE(tendered_amount, amount)
WHERE informed_amount IS NULL;

UPDATE payments
SET applied_amount = amount
WHERE applied_amount IS NULL;

UPDATE payments
SET change_amount = CASE
    WHEN method = 'CASH' AND tendered_amount IS NOT NULL AND tendered_amount > amount
        THEN ROUND(tendered_amount - amount, 2)
    ELSE 0
END
WHERE change_amount IS NULL;

ALTER TABLE payments
    ALTER COLUMN informed_amount SET NOT NULL,
    ALTER COLUMN informed_amount SET DEFAULT 0,
    ALTER COLUMN applied_amount SET NOT NULL,
    ALTER COLUMN applied_amount SET DEFAULT 0,
    ALTER COLUMN change_amount SET NOT NULL,
    ALTER COLUMN change_amount SET DEFAULT 0;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS uk_payments_idempotency_key;
ALTER TABLE payments
    ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payments_informed_non_negative;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payments_applied_non_negative;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payments_change_non_negative;
ALTER TABLE payments
    ADD CONSTRAINT ck_payments_informed_non_negative CHECK (informed_amount >= 0),
    ADD CONSTRAINT ck_payments_applied_non_negative CHECK (applied_amount >= 0),
    ADD CONSTRAINT ck_payments_change_non_negative CHECK (change_amount >= 0);

CREATE INDEX IF NOT EXISTS idx_payments_nsu ON payments (nsu) WHERE nsu IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payments_auth_code ON payments (authorization_code) WHERE authorization_code IS NOT NULL;

COMMENT ON COLUMN payments.informed_amount IS 'Valor informado pelo operador/cliente';
COMMENT ON COLUMN payments.applied_amount IS 'Valor aplicado à venda (calculado pela API)';
COMMENT ON COLUMN payments.change_amount IS 'Troco oficial; somente dinheiro';
COMMENT ON COLUMN payments.nsu IS 'NSU / referência TEF (sem PAN/CVV)';
COMMENT ON COLUMN payments.card_brand IS 'Bandeira (dados mínimos; sem número do cartão)';
COMMENT ON COLUMN payments.acquirer IS 'Adquirente';
COMMENT ON COLUMN payments.idempotency_key IS 'Evita pagamento duplicado';
