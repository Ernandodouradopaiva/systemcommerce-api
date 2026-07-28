-- V135: cancelamentos orquestrados do PDV, estornos com falha controlada e devoluções

CREATE TABLE sale_cancellations (
    id                      UUID            PRIMARY KEY,
    sale_id                 UUID            NOT NULL REFERENCES sales (id),
    status                  VARCHAR(30)     NOT NULL,
    reason                  VARCHAR(500)    NOT NULL,
    requested_by_id         UUID            NOT NULL REFERENCES users (id),
    authorized_by_id        UUID            NULL REFERENCES users (id),
    executed_by_id          UUID            NULL REFERENCES users (id),
    requested_at            TIMESTAMPTZ     NOT NULL,
    authorized_at           TIMESTAMPTZ     NULL,
    executed_at             TIMESTAMPTZ     NULL,
    decision_notes          VARCHAR(500)    NULL,
    failure_detail          VARCHAR(1000)   NULL,
    idempotency_key         VARCHAR(100)    NULL,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT ck_sale_cancellations_status CHECK (status IN (
        'REQUESTED', 'AUTHORIZED', 'COMPLETED', 'REJECTED', 'PARTIALLY_FAILED'
    ))
);

CREATE UNIQUE INDEX uk_sale_cancellations_idempotency
    ON sale_cancellations (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_sale_cancellations_sale ON sale_cancellations (sale_id);
CREATE INDEX idx_sale_cancellations_status ON sale_cancellations (status);
CREATE INDEX idx_sale_cancellations_requested_at ON sale_cancellations (requested_at DESC);

COMMENT ON TABLE sale_cancellations IS 'Workflow de cancelamento PDV (solicitação → autorização → execução)';
COMMENT ON COLUMN sale_cancellations.status IS 'REQUESTED|AUTHORIZED|COMPLETED|REJECTED|PARTIALLY_FAILED';

CREATE TABLE cancellation_refunds (
    id                      UUID            PRIMARY KEY,
    cancellation_id         UUID            NOT NULL REFERENCES sale_cancellations (id),
    payment_id              UUID            NOT NULL REFERENCES payments (id),
    status                  VARCHAR(30)     NOT NULL,
    method                  VARCHAR(20)     NOT NULL,
    amount                  NUMERIC(19, 2)  NOT NULL,
    failure_reason          VARCHAR(500)    NULL,
    attempts                INTEGER         NOT NULL DEFAULT 0,
    last_attempt_at         TIMESTAMPTZ     NULL,
    completed_at            TIMESTAMPTZ     NULL,
    idempotency_key         VARCHAR(100)    NULL,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT ck_cancellation_refunds_status CHECK (status IN (
        'PENDING', 'COMPLETED', 'FAILED'
    )),
    CONSTRAINT ck_cancellation_refunds_amount CHECK (amount >= 0),
    CONSTRAINT uk_cancellation_refunds_payment UNIQUE (cancellation_id, payment_id)
);

CREATE UNIQUE INDEX uk_cancellation_refunds_idempotency
    ON cancellation_refunds (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_cancellation_refunds_cancellation ON cancellation_refunds (cancellation_id);
CREATE INDEX idx_cancellation_refunds_status ON cancellation_refunds (status);

COMMENT ON TABLE cancellation_refunds IS 'Estornos por pagamento no cancelamento; PENDING/FAILED permitem reprocessamento';

CREATE TABLE sale_returns (
    id                      UUID            PRIMARY KEY,
    return_number           VARCHAR(40)     NOT NULL,
    original_sale_id        UUID            NOT NULL REFERENCES sales (id),
    cash_session_id         UUID            NULL REFERENCES cash_sessions (id),
    status                  VARCHAR(30)     NOT NULL,
    reason                  VARCHAR(500)    NOT NULL,
    requested_by_id         UUID            NOT NULL REFERENCES users (id),
    confirmed_at            TIMESTAMPTZ     NULL,
    notes                   VARCHAR(1000)   NULL,
    idempotency_key         VARCHAR(100)    NULL,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_sale_returns_number UNIQUE (return_number),
    CONSTRAINT ck_sale_returns_status CHECK (status IN (
        'CONFIRMED', 'CANCELLED'
    ))
);

CREATE UNIQUE INDEX uk_sale_returns_idempotency
    ON sale_returns (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_sale_returns_original_sale ON sale_returns (original_sale_id);
CREATE INDEX idx_sale_returns_status ON sale_returns (status);

COMMENT ON TABLE sale_returns IS 'Documento próprio de devolução futura (não edita a venda original)';

CREATE TABLE sale_return_items (
    id                      UUID            PRIMARY KEY,
    sale_return_id          UUID            NOT NULL REFERENCES sale_returns (id),
    product_id              UUID            NOT NULL REFERENCES products (id),
    original_sale_item_id   UUID            NULL REFERENCES sale_items (id),
    quantity                NUMERIC(19, 3)  NOT NULL,
    unit_price              NUMERIC(19, 2)  NOT NULL,
    line_total              NUMERIC(19, 2)  NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT ck_sale_return_items_qty CHECK (quantity > 0),
    CONSTRAINT ck_sale_return_items_price CHECK (unit_price >= 0),
    CONSTRAINT ck_sale_return_items_total CHECK (line_total >= 0)
);

CREATE INDEX idx_sale_return_items_return ON sale_return_items (sale_return_id);
CREATE INDEX idx_sale_return_items_product ON sale_return_items (product_id);

COMMENT ON TABLE sale_return_items IS 'Itens da devolução; estoque via FUTURE_RETURN';
