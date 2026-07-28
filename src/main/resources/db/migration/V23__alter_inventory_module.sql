-- V23: módulo de estoque — motivos de ajuste, tipos ampliados e saldo condicionalmente negativo
CREATE TABLE inventory_adjustment_reasons (
    id              UUID            NOT NULL,
    code            VARCHAR(40)     NOT NULL,
    description     VARCHAR(200)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory_adjustment_reasons PRIMARY KEY (id),
    CONSTRAINT uk_inventory_adjustment_reasons_code UNIQUE (code)
);

CREATE INDEX idx_inventory_adjustment_reasons_active ON inventory_adjustment_reasons (active);

COMMENT ON TABLE inventory_adjustment_reasons IS 'Motivos obrigatórios para ajustes de estoque';

ALTER TABLE stock_movements
    ADD COLUMN IF NOT EXISTS observation VARCHAR(1000) NULL,
    ADD COLUMN IF NOT EXISTS adjustment_reason_id UUID NULL;

ALTER TABLE stock_movements
    DROP CONSTRAINT IF EXISTS ck_stock_movements_type,
    DROP CONSTRAINT IF EXISTS ck_stock_movements_previous_non_negative,
    DROP CONSTRAINT IF EXISTS ck_stock_movements_new_non_negative;

UPDATE stock_movements SET type = 'ENTRY' WHERE type = 'IN';
UPDATE stock_movements SET type = 'EXIT' WHERE type = 'OUT';
UPDATE stock_movements SET type = 'CORRECTION' WHERE type = 'ADJUSTMENT';

ALTER TABLE stock_movements
    ADD CONSTRAINT ck_stock_movements_type CHECK (type IN (
        'ENTRY',
        'EXIT',
        'ADJUSTMENT_POSITIVE',
        'ADJUSTMENT_NEGATIVE',
        'SALE',
        'SALE_CANCEL',
        'FUTURE_RETURN',
        'CORRECTION'
    )),
    ADD CONSTRAINT fk_stock_movements_adjustment_reason
        FOREIGN KEY (adjustment_reason_id) REFERENCES inventory_adjustment_reasons (id);

ALTER TABLE inventory
    DROP CONSTRAINT IF EXISTS ck_inventory_quantity_non_negative;

CREATE INDEX IF NOT EXISTS idx_stock_movements_adjustment_reason_id
    ON stock_movements (adjustment_reason_id);

COMMENT ON COLUMN stock_movements.observation IS 'Observação livre da movimentação';
COMMENT ON COLUMN stock_movements.adjustment_reason_id IS 'Motivo formal quando a movimentação é um ajuste';
COMMENT ON COLUMN stock_movements.reference_type IS 'Origem da movimentação (MANUAL, SALE, SEED, etc.)';
COMMENT ON COLUMN stock_movements.reference_id IS 'Identificador da origem (ex.: id da venda)';
