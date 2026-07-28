-- V122: vínculo opcional de pagamento à sessão de caixa (PDV)
ALTER TABLE payments
    ADD COLUMN cash_session_id UUID NULL;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id);

CREATE INDEX idx_payments_cash_session_id ON payments (cash_session_id);

COMMENT ON COLUMN payments.cash_session_id IS 'Sessão de caixa do recebimento POS (nullable no backoffice)';
