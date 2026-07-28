-- V116: movimentações de estoque vinculadas ao depósito
ALTER TABLE stock_movements
    ADD COLUMN warehouse_id UUID NULL;

UPDATE stock_movements sm
SET warehouse_id = (
    SELECT w.id
    FROM warehouses w
    JOIN stores s ON s.id = w.store_id
    WHERE s.code = 'LOJA-01' AND w.code = 'DEP-01'
    LIMIT 1
)
WHERE sm.warehouse_id IS NULL;

ALTER TABLE stock_movements
    ALTER COLUMN warehouse_id SET NOT NULL;

ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id);

CREATE INDEX idx_stock_movements_warehouse_id ON stock_movements (warehouse_id);

COMMENT ON COLUMN stock_movements.warehouse_id IS 'Depósito afetado pela movimentação';
