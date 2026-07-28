-- V127: extensões POS na venda central (channel, sessão, terminal, SUSPENDED)

ALTER TABLE sales

    ADD COLUMN IF NOT EXISTS channel VARCHAR(20) NOT NULL DEFAULT 'ADMIN',

    ADD COLUMN IF NOT EXISTS store_id UUID NULL,

    ADD COLUMN IF NOT EXISTS terminal_id UUID NULL,

    ADD COLUMN IF NOT EXISTS cash_session_id UUID NULL,

    ADD COLUMN IF NOT EXISTS warehouse_id UUID NULL,

    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100) NULL,

    ADD COLUMN IF NOT EXISTS last_operation_idempotency_key VARCHAR(100) NULL,

    ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ NULL,

    ADD COLUMN IF NOT EXISTS suspend_reason VARCHAR(500) NULL;



ALTER TABLE sales DROP CONSTRAINT IF EXISTS ck_sales_channel;

ALTER TABLE sales

    ADD CONSTRAINT ck_sales_channel CHECK (channel IN ('ADMIN', 'POS'));



ALTER TABLE sales DROP CONSTRAINT IF EXISTS ck_sales_status;

ALTER TABLE sales

    ADD CONSTRAINT ck_sales_status CHECK (status IN (

        'DRAFT', 'SUSPENDED', 'CONFIRMED', 'PAID', 'PARTIALLY_PAID', 'CANCELLED'

    ));



ALTER TABLE sales DROP CONSTRAINT IF EXISTS uk_sales_idempotency_key;

ALTER TABLE sales

    ADD CONSTRAINT uk_sales_idempotency_key UNIQUE (idempotency_key);



ALTER TABLE sales DROP CONSTRAINT IF EXISTS fk_sales_store;

ALTER TABLE sales

    ADD CONSTRAINT fk_sales_store FOREIGN KEY (store_id) REFERENCES stores (id);



ALTER TABLE sales DROP CONSTRAINT IF EXISTS fk_sales_terminal;

ALTER TABLE sales

    ADD CONSTRAINT fk_sales_terminal FOREIGN KEY (terminal_id) REFERENCES pos_terminals (id);



ALTER TABLE sales DROP CONSTRAINT IF EXISTS fk_sales_cash_session;

ALTER TABLE sales

    ADD CONSTRAINT fk_sales_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id);



ALTER TABLE sales DROP CONSTRAINT IF EXISTS fk_sales_warehouse;

ALTER TABLE sales

    ADD CONSTRAINT fk_sales_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id);



ALTER TABLE sale_status_history DROP CONSTRAINT IF EXISTS ck_sale_status_history_from;

ALTER TABLE sale_status_history DROP CONSTRAINT IF EXISTS ck_sale_status_history_to;

ALTER TABLE sale_status_history

    ADD CONSTRAINT ck_sale_status_history_from CHECK (

        from_status IS NULL OR from_status IN (

            'DRAFT', 'SUSPENDED', 'CONFIRMED', 'PAID', 'PARTIALLY_PAID', 'CANCELLED'

        )

    ),

    ADD CONSTRAINT ck_sale_status_history_to CHECK (

        to_status IN (

            'DRAFT', 'SUSPENDED', 'CONFIRMED', 'PAID', 'PARTIALLY_PAID', 'CANCELLED'

        )

    );



CREATE INDEX IF NOT EXISTS idx_sales_channel ON sales (channel);

CREATE INDEX IF NOT EXISTS idx_sales_cash_session ON sales (cash_session_id);

CREATE INDEX IF NOT EXISTS idx_sales_terminal_status ON sales (terminal_id, status);

CREATE INDEX IF NOT EXISTS idx_sales_seller_status ON sales (seller_id, status);



COMMENT ON COLUMN sales.channel IS 'Canal da venda: ADMIN (backoffice) ou POS (PDV)';

COMMENT ON COLUMN sales.cash_session_id IS 'Sessão de caixa obrigatória para channel=POS';

COMMENT ON COLUMN sales.warehouse_id IS 'Depósito de origem (baixa de estoque na confirmação)';

COMMENT ON COLUMN sales.idempotency_key IS 'Chave de idempotência da abertura da venda';


