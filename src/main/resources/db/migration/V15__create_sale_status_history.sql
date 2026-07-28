-- V15: histórico de status de vendas
CREATE TABLE sale_status_history (
    id                  UUID            NOT NULL,
    sale_id             UUID            NOT NULL,
    from_status         VARCHAR(20)     NULL,
    to_status           VARCHAR(20)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    changed_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by          UUID            NULL,
    CONSTRAINT pk_sale_status_history PRIMARY KEY (id),
    CONSTRAINT fk_sale_status_history_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_status_history_user FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT ck_sale_status_history_from CHECK (
        from_status IS NULL OR from_status IN ('DRAFT', 'CONFIRMED', 'PAID', 'CANCELLED')
    ),
    CONSTRAINT ck_sale_status_history_to CHECK (to_status IN ('DRAFT', 'CONFIRMED', 'PAID', 'CANCELLED'))
);

CREATE INDEX idx_sale_status_history_sale_id ON sale_status_history (sale_id);
CREATE INDEX idx_sale_status_history_changed_at ON sale_status_history (changed_at);
CREATE INDEX idx_sale_status_history_to_status ON sale_status_history (to_status);

COMMENT ON TABLE sale_status_history IS 'Trilha imutável de mudanças de status da venda';
