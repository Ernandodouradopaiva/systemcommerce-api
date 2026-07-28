-- V12: vendas
CREATE TABLE sales (
    id                  UUID            NOT NULL,
    customer_id         UUID            NOT NULL,
    seller_id           UUID            NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    subtotal            NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    notes               VARCHAR(1000)   NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sales PRIMARY KEY (id),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sales_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT ck_sales_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'PAID', 'CANCELLED')),
    CONSTRAINT ck_sales_subtotal_non_negative CHECK (subtotal >= 0),
    CONSTRAINT ck_sales_discount_non_negative CHECK (discount_amount >= 0),
    CONSTRAINT ck_sales_total_non_negative CHECK (total_amount >= 0),
    CONSTRAINT ck_sales_discount_lte_subtotal CHECK (discount_amount <= subtotal)
);

CREATE INDEX idx_sales_customer_id ON sales (customer_id);
CREATE INDEX idx_sales_seller_id ON sales (seller_id);
CREATE INDEX idx_sales_status ON sales (status);
CREATE INDEX idx_sales_created_at ON sales (created_at);
CREATE INDEX idx_sales_active ON sales (active);

COMMENT ON TABLE sales IS 'Cabeçalho de vendas; totais sempre calculados na API';
COMMENT ON COLUMN sales.status IS 'DRAFT | CONFIRMED | PAID | CANCELLED';
