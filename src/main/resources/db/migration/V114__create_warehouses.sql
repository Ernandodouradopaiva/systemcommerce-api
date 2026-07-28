-- V114: depósitos / locais de estoque
CREATE TABLE warehouses (
    id                  UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    allows_sale         BOOLEAN         NOT NULL DEFAULT TRUE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_warehouses PRIMARY KEY (id),
    CONSTRAINT uk_warehouses_store_code UNIQUE (store_id, code),
    CONSTRAINT fk_warehouses_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_warehouses_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_warehouses_code_not_blank CHECK (LENGTH(TRIM(code)) > 0)
);

CREATE INDEX idx_warehouses_store_id ON warehouses (store_id);
CREATE INDEX idx_warehouses_status ON warehouses (status);
CREATE INDEX idx_warehouses_allows_sale ON warehouses (allows_sale) WHERE allows_sale = TRUE;

COMMENT ON TABLE warehouses IS 'Depósitos / locais de estoque por loja';
COMMENT ON COLUMN warehouses.allows_sale IS 'Quando TRUE, o depósito pode ser vinculado a terminal PDV e baixar estoque de venda';
