-- V176: pedidos de venda (Prompt 58) — compromisso comercial; não baixa estoque na criação
CREATE TABLE store_sales_order_sequences (
    store_id    UUID            NOT NULL,
    last_value  BIGINT          NOT NULL DEFAULT 0,
    prefix      VARCHAR(10)     NOT NULL DEFAULT 'P',
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_sales_order_sequences PRIMARY KEY (store_id),
    CONSTRAINT fk_store_sales_order_sequences_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE sales_orders (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    warehouse_id        UUID            NULL,
    order_number        VARCHAR(40)     NOT NULL,
    quote_id            UUID            NULL,
    customer_id         UUID            NULL,
    seller_id           UUID            NULL,
    carrier_name        VARCHAR(200)    NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    notes               VARCHAR(2000)   NULL,
    reserve_stock       BOOLEAN         NOT NULL DEFAULT FALSE,
    subtotal_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    freight_amount      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    total_amount        NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    generated_sale_id   UUID            NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sales_orders PRIMARY KEY (id),
    CONSTRAINT uk_sales_orders_number UNIQUE (order_number),
    CONSTRAINT fk_sales_orders_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_sales_orders_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_sales_orders_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_sales_orders_quote FOREIGN KEY (quote_id) REFERENCES quotes (id),
    CONSTRAINT fk_sales_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sales_orders_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_sales_orders_sale FOREIGN KEY (generated_sale_id) REFERENCES sales (id),
    CONSTRAINT ck_sales_orders_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'PICKING', 'PICKED', 'INVOICED', 'DELIVERED', 'CANCELLED'
    )),
    CONSTRAINT ck_sales_orders_amounts_non_negative CHECK (
        subtotal_amount >= 0 AND discount_amount >= 0 AND freight_amount >= 0 AND total_amount >= 0
    )
);

ALTER TABLE quotes
    ADD CONSTRAINT fk_quotes_converted_sales_order
        FOREIGN KEY (converted_sales_order_id) REFERENCES sales_orders (id);

CREATE TABLE sales_order_items (
    id                  UUID            NOT NULL,
    sales_order_id      UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    line_number         INT             NOT NULL,
    description         VARCHAR(300)    NULL,
    quantity            NUMERIC(18, 4)  NOT NULL,
    unit_price          NUMERIC(18, 2)  NOT NULL,
    discount_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    line_subtotal       NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    line_total          NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sales_order_items PRIMARY KEY (id),
    CONSTRAINT uk_sales_order_items_line UNIQUE (sales_order_id, line_number),
    CONSTRAINT fk_sales_order_items_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_sales_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_sales_order_items_qty_positive CHECK (quantity > 0),
    CONSTRAINT ck_sales_order_items_price_non_negative CHECK (unit_price >= 0)
);

CREATE TABLE sales_order_status_history (
    id              UUID            NOT NULL,
    sales_order_id  UUID            NOT NULL,
    from_status     VARCHAR(30)     NULL,
    to_status       VARCHAR(30)     NOT NULL,
    notes           VARCHAR(1000)   NULL,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by      UUID            NULL,
    CONSTRAINT pk_sales_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_sales_order_status_history_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_sales_orders_store ON sales_orders (store_id);
CREATE INDEX idx_sales_orders_customer ON sales_orders (customer_id);
CREATE INDEX idx_sales_orders_status ON sales_orders (status);
CREATE INDEX idx_sales_orders_quote ON sales_orders (quote_id);
CREATE INDEX idx_sales_order_items_order ON sales_order_items (sales_order_id);
CREATE INDEX idx_sales_order_status_history_order ON sales_order_status_history (sales_order_id, changed_at);

COMMENT ON TABLE sales_orders IS 'Pedidos de venda — compromisso comercial; faturamento gera Sale (Prompt 58)';
