-- V11: movimentações de estoque (append-only)
CREATE TABLE stock_movements (
    id                  UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    quantity            NUMERIC(19, 3)  NOT NULL,
    previous_quantity   NUMERIC(19, 3)  NOT NULL,
    new_quantity        NUMERIC(19, 3)  NOT NULL,
    reference_type      VARCHAR(40)     NULL,
    reference_id        UUID            NULL,
    reason              VARCHAR(500)    NULL,
    user_id             UUID            NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_stock_movements_type CHECK (type IN ('IN', 'OUT', 'ADJUSTMENT')),
    CONSTRAINT ck_stock_movements_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_stock_movements_previous_non_negative CHECK (previous_quantity >= 0),
    CONSTRAINT ck_stock_movements_new_non_negative CHECK (new_quantity >= 0)
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements (product_id);
CREATE INDEX idx_stock_movements_type ON stock_movements (type);
CREATE INDEX idx_stock_movements_created_at ON stock_movements (created_at);
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference_type, reference_id);
CREATE INDEX idx_stock_movements_user_id ON stock_movements (user_id);

COMMENT ON TABLE stock_movements IS 'Histórico imutável de movimentações de estoque';
COMMENT ON COLUMN stock_movements.reference_type IS 'Origem da movimentação (ex.: SALE, MANUAL, CANCELLATION)';
