-- V13: itens de venda
CREATE TABLE sale_items (
    id                  UUID            NOT NULL,
    sale_id             UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    quantity            NUMERIC(19, 3)  NOT NULL,
    unit_price          NUMERIC(19, 2)  NOT NULL,
    discount_amount     NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    line_total          NUMERIC(19, 2)  NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sale_items PRIMARY KEY (id),
    CONSTRAINT fk_sale_items_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_sale_items_sale_product UNIQUE (sale_id, product_id),
    CONSTRAINT ck_sale_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_sale_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_sale_items_discount_non_negative CHECK (discount_amount >= 0),
    CONSTRAINT ck_sale_items_line_total_non_negative CHECK (line_total >= 0)
);

CREATE INDEX idx_sale_items_sale_id ON sale_items (sale_id);
CREATE INDEX idx_sale_items_product_id ON sale_items (product_id);

COMMENT ON TABLE sale_items IS 'Itens de uma venda; preços e totais validados pela API';
