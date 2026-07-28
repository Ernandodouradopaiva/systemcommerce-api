-- V124: evolução de movimentações de caixa (Prompt 34)
ALTER TABLE cash_movements DROP CONSTRAINT IF EXISTS ck_cash_movements_type;

UPDATE cash_movements SET type = 'OPENING' WHERE type = 'OPENING_FLOAT';
UPDATE cash_movements SET type = 'SUPPLY' WHERE type = 'CASH_SUPPLY';
UPDATE cash_movements SET type = 'WITHDRAWAL' WHERE type = 'CASH_WITHDRAWAL';

ALTER TABLE cash_movements
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000) NULL,
    ADD COLUMN IF NOT EXISTS reason_id UUID NULL,
    ADD COLUMN IF NOT EXISTS executed_by_id UUID NULL,
    ADD COLUMN IF NOT EXISTS sale_id UUID NULL,
    ADD COLUMN IF NOT EXISTS origin_type VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS origin_id UUID NULL,
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS reverses_movement_id UUID NULL,
    ADD COLUMN IF NOT EXISTS cash_effect VARCHAR(20) NULL;

UPDATE cash_movements
SET description = COALESCE(description, notes, reason),
    occurred_at = COALESCE(occurred_at, created_at)
WHERE occurred_at IS NULL OR description IS NULL;

UPDATE cash_movements m
SET executed_by_id = s.operator_id
FROM cash_sessions s
WHERE m.cash_session_id = s.id
  AND m.executed_by_id IS NULL;

ALTER TABLE cash_movements
    ALTER COLUMN occurred_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

ALTER TABLE cash_movements
    ALTER COLUMN occurred_at SET NOT NULL;

ALTER TABLE cash_movements
    ADD CONSTRAINT ck_cash_movements_type CHECK (type IN (
        'OPENING', 'SUPPLY', 'WITHDRAWAL', 'CASH_SALE', 'CASH_REFUND', 'ADJUSTMENT', 'CLOSING'
    ));

ALTER TABLE cash_movements
    ADD CONSTRAINT ck_cash_movements_cash_effect CHECK (
        cash_effect IS NULL OR cash_effect IN ('INCREASE', 'DECREASE')
    );

ALTER TABLE cash_movements
    ADD CONSTRAINT fk_cash_movements_executed_by FOREIGN KEY (executed_by_id) REFERENCES users (id);

ALTER TABLE cash_movements
    ADD CONSTRAINT fk_cash_movements_sale FOREIGN KEY (sale_id) REFERENCES sales (id);

ALTER TABLE cash_movements
    ADD CONSTRAINT fk_cash_movements_reverses FOREIGN KEY (reverses_movement_id) REFERENCES cash_movements (id);

CREATE INDEX IF NOT EXISTS idx_cash_movements_reason_id ON cash_movements (reason_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_executed_by ON cash_movements (executed_by_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_sale_id ON cash_movements (sale_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_origin ON cash_movements (origin_type, origin_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_reverses ON cash_movements (reverses_movement_id);

COMMENT ON COLUMN cash_movements.cash_effect IS 'Obrigatório para ADJUSTMENT: INCREASE ou DECREASE no saldo físico';
COMMENT ON COLUMN cash_movements.reverses_movement_id IS 'Movimentação original estornada por esta (imutabilidade via movimento inverso)';
