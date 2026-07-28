-- V121: movimentações de caixa (fundo, suprimento, sangria)
CREATE TABLE cash_movements (
    id                      UUID            NOT NULL,
    cash_session_id         UUID            NOT NULL,
    type                    VARCHAR(30)     NOT NULL,
    amount                  NUMERIC(19, 2)  NOT NULL,
    reason                  VARCHAR(500)    NULL,
    notes                   VARCHAR(1000)   NULL,
    authorized_by_id        UUID            NULL,
    idempotency_key         VARCHAR(100)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_cash_movements PRIMARY KEY (id),
    CONSTRAINT fk_cash_movements_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id),
    CONSTRAINT fk_cash_movements_authorized_by FOREIGN KEY (authorized_by_id) REFERENCES users (id),
    CONSTRAINT ck_cash_movements_type CHECK (type IN (
        'OPENING_FLOAT', 'CASH_SUPPLY', 'CASH_WITHDRAWAL'
    )),
    CONSTRAINT ck_cash_movements_amount_positive CHECK (amount > 0),
    CONSTRAINT uk_cash_movements_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_cash_movements_session_id ON cash_movements (cash_session_id);
CREATE INDEX idx_cash_movements_type ON cash_movements (type);
CREATE INDEX idx_cash_movements_created_at ON cash_movements (created_at);

COMMENT ON TABLE cash_movements IS 'Movimentações de numerário do caixa (não alteram estoque de produto)';
