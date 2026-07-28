-- V175: orçamentos (Prompt 57) — não baixa estoque; não gera financeiro
CREATE TABLE store_quote_sequences (
    store_id    UUID            NOT NULL,
    last_value  BIGINT          NOT NULL DEFAULT 0,
    prefix      VARCHAR(10)     NOT NULL DEFAULT 'O',
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_quote_sequences PRIMARY KEY (store_id),
    CONSTRAINT fk_store_quote_sequences_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE quotes (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    quote_number        VARCHAR(40)     NOT NULL,
    customer_id         UUID            NULL,
    seller_id           UUID            NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    valid_until         DATE            NULL,
    notes               VARCHAR(2000)   NULL,
    reserve_stock       BOOLEAN         NOT NULL DEFAULT FALSE,
    subtotal_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    freight_amount      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    total_amount        NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    converted_sales_order_id UUID       NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_quotes PRIMARY KEY (id),
    CONSTRAINT uk_quotes_number UNIQUE (quote_number),
    CONSTRAINT fk_quotes_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_quotes_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_quotes_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_quotes_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT ck_quotes_status CHECK (status IN (
        'DRAFT', 'UNDER_REVIEW', 'SENT', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'CONVERTED'
    )),
    CONSTRAINT ck_quotes_amounts_non_negative CHECK (
        subtotal_amount >= 0 AND discount_amount >= 0 AND freight_amount >= 0 AND total_amount >= 0
    )
);

CREATE TABLE quote_items (
    id                  UUID            NOT NULL,
    quote_id            UUID            NOT NULL,
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
    CONSTRAINT pk_quote_items PRIMARY KEY (id),
    CONSTRAINT uk_quote_items_line UNIQUE (quote_id, line_number),
    CONSTRAINT fk_quote_items_quote FOREIGN KEY (quote_id) REFERENCES quotes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quote_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_quote_items_qty_positive CHECK (quantity > 0),
    CONSTRAINT ck_quote_items_price_non_negative CHECK (unit_price >= 0)
);

CREATE TABLE quote_status_history (
    id              UUID            NOT NULL,
    quote_id        UUID            NOT NULL,
    from_status     VARCHAR(30)     NULL,
    to_status       VARCHAR(30)     NOT NULL,
    notes           VARCHAR(1000)   NULL,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by      UUID            NULL,
    CONSTRAINT pk_quote_status_history PRIMARY KEY (id),
    CONSTRAINT fk_quote_status_history_quote FOREIGN KEY (quote_id) REFERENCES quotes (id) ON DELETE CASCADE
);

CREATE INDEX idx_quotes_store ON quotes (store_id);
CREATE INDEX idx_quotes_customer ON quotes (customer_id);
CREATE INDEX idx_quotes_status ON quotes (status);
CREATE INDEX idx_quotes_valid_until ON quotes (valid_until);
CREATE INDEX idx_quote_items_quote ON quote_items (quote_id);
CREATE INDEX idx_quote_status_history_quote ON quote_status_history (quote_id, changed_at);

COMMENT ON TABLE quotes IS 'Orçamentos comerciais — sem baixa de estoque e sem financeiro (Prompt 57)';
