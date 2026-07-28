-- V178: histórico de faturamento de pedidos de venda (Prompt 59)
CREATE TABLE sales_order_billing_history (
    id                  UUID            NOT NULL,
    sales_order_id      UUID            NOT NULL,
    sale_id             UUID            NULL,
    event_type          VARCHAR(40)     NOT NULL,
    notes               VARCHAR(1000)   NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    performed_by        UUID            NULL,
    CONSTRAINT pk_sales_order_billing_history PRIMARY KEY (id),
    CONSTRAINT fk_sales_order_billing_history_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_sales_order_billing_history_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT ck_sales_order_billing_event CHECK (event_type IN (
        'BILLING_STARTED', 'SALE_CREATED', 'SALE_CONFIRMED', 'STOCK_MOVED', 'BILLING_COMPLETED', 'BILLING_FAILED'
    ))
);

CREATE INDEX idx_sales_order_billing_history_order ON sales_order_billing_history (sales_order_id, occurred_at);

COMMENT ON TABLE sales_order_billing_history IS 'Histórico obrigatório do faturamento (Prompt 59) — efetiva venda + estoque';
