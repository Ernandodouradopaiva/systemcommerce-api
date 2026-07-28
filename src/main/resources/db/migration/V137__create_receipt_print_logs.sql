-- V137: registro de impressão / reimpressão de comprovantes PDV

CREATE TABLE IF NOT EXISTS receipt_print_logs (
    id                  UUID PRIMARY KEY,
    print_type          VARCHAR(40)  NOT NULL,
    sequence_no         INTEGER      NOT NULL,
    sale_id             UUID         REFERENCES sales (id),
    payment_id          UUID         REFERENCES payments (id),
    cash_session_id     UUID         REFERENCES cash_sessions (id),
    cash_movement_id    UUID         REFERENCES cash_movements (id),
    sale_cancellation_id UUID REFERENCES sale_cancellations (id),
    requested_by_id     UUID         NOT NULL REFERENCES users (id),
    reason              VARCHAR(500),
    copies              INTEGER      NOT NULL DEFAULT 1,
    layout              VARCHAR(40)  NOT NULL,
    is_reprint          BOOLEAN      NOT NULL DEFAULT FALSE,
    original_log_id     UUID         REFERENCES receipt_print_logs (id),
    authentication_id   VARCHAR(80)  NOT NULL,
    terminal_id         UUID         REFERENCES pos_terminals (id),
    notes               VARCHAR(1000),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_receipt_print_logs_sale ON receipt_print_logs (sale_id);
CREATE INDEX IF NOT EXISTS idx_receipt_print_logs_session ON receipt_print_logs (cash_session_id);
CREATE INDEX IF NOT EXISTS idx_receipt_print_logs_created ON receipt_print_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_receipt_print_logs_type ON receipt_print_logs (print_type);

CREATE SEQUENCE IF NOT EXISTS receipt_print_sequence START WITH 1 INCREMENT BY 1;
