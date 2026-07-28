-- V120: sessões de caixa (PDV)
CREATE TABLE cash_sessions (
    id                      UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    terminal_id             UUID            NOT NULL,
    operator_id             UUID            NOT NULL,
    opened_at               TIMESTAMPTZ     NOT NULL,
    closed_at               TIMESTAMPTZ     NULL,
    opening_amount          NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    status                  VARCHAR(20)     NOT NULL,
    expected_amount         NUMERIC(19, 2)  NULL,
    counted_amount          NUMERIC(19, 2)  NULL,
    difference_amount       NUMERIC(19, 2)  NULL,
    opening_notes           VARCHAR(1000)   NULL,
    closing_notes           VARCHAR(1000)   NULL,
    authorized_by_id        UUID            NULL,
    open_idempotency_key    VARCHAR(100)    NULL,
    close_idempotency_key   VARCHAR(100)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_cash_sessions PRIMARY KEY (id),
    CONSTRAINT fk_cash_sessions_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_cash_sessions_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals (id),
    CONSTRAINT fk_cash_sessions_operator FOREIGN KEY (operator_id) REFERENCES users (id),
    CONSTRAINT fk_cash_sessions_authorized_by FOREIGN KEY (authorized_by_id) REFERENCES users (id),
    CONSTRAINT ck_cash_sessions_status CHECK (status IN ('OPEN', 'CLOSING', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_cash_sessions_opening_non_negative CHECK (opening_amount >= 0),
    CONSTRAINT uk_cash_sessions_open_idempotency UNIQUE (open_idempotency_key)
);

-- No máximo uma sessão operacional (OPEN/CLOSING) por terminal
CREATE UNIQUE INDEX uk_cash_sessions_terminal_active
    ON cash_sessions (terminal_id)
    WHERE status IN ('OPEN', 'CLOSING');

CREATE INDEX idx_cash_sessions_store_id ON cash_sessions (store_id);
CREATE INDEX idx_cash_sessions_terminal_id ON cash_sessions (terminal_id);
CREATE INDEX idx_cash_sessions_operator_id ON cash_sessions (operator_id);
CREATE INDEX idx_cash_sessions_status ON cash_sessions (status);
CREATE INDEX idx_cash_sessions_opened_at ON cash_sessions (opened_at);

COMMENT ON TABLE cash_sessions IS 'Sessões de caixa do PDV';
COMMENT ON COLUMN cash_sessions.expected_amount IS 'Valor esperado em dinheiro (calculado na API no fechamento)';
COMMENT ON COLUMN cash_sessions.counted_amount IS 'Valor informado pelo operador na conferência';
COMMENT ON COLUMN cash_sessions.difference_amount IS 'counted_amount - expected_amount (calculado na API)';
