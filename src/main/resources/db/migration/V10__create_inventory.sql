-- V10: estoque (saldo atual por produto)
CREATE TABLE inventory (
    id                  UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    quantity            NUMERIC(19, 3)  NOT NULL DEFAULT 0,
    minimum_quantity    NUMERIC(19, 3)  NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory PRIMARY KEY (id),
    CONSTRAINT uk_inventory_product UNIQUE (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_inventory_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT ck_inventory_minimum_non_negative CHECK (minimum_quantity >= 0)
);

CREATE INDEX idx_inventory_product_id ON inventory (product_id);
CREATE INDEX idx_inventory_quantity ON inventory (quantity);
CREATE INDEX idx_inventory_low_stock ON inventory (quantity, minimum_quantity);

COMMENT ON TABLE inventory IS 'Saldo atual de estoque por produto';
COMMENT ON COLUMN inventory.minimum_quantity IS 'Quantidade mínima para alerta de reposição';
