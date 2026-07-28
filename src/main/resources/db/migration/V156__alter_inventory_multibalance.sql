-- V156: estoque multilojas/multidepósito — saldos compostos + loja denormalizada
-- Chave lógica permanece (product_id, warehouse_id); loja deriva do depósito.

ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS quantity_reserved NUMERIC(19, 3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quantity_blocked NUMERIC(19, 3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quantity_in_transit NUMERIC(19, 3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS maximum_quantity NUMERIC(19, 3),
    ADD COLUMN IF NOT EXISTS reorder_point NUMERIC(19, 3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS store_id UUID;

UPDATE inventory i
SET store_id = w.store_id
FROM warehouses w
WHERE i.warehouse_id = w.id
  AND (i.store_id IS NULL OR i.store_id <> w.store_id);

UPDATE inventory
SET reorder_point = COALESCE(minimum_quantity, 0)
WHERE reorder_point = 0
  AND minimum_quantity IS NOT NULL
  AND minimum_quantity > 0;

ALTER TABLE inventory
    ALTER COLUMN store_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_store'
    ) THEN
        ALTER TABLE inventory
            ADD CONSTRAINT fk_inventory_store
                FOREIGN KEY (store_id) REFERENCES stores (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_inventory_store_id ON inventory (store_id);
CREATE INDEX IF NOT EXISTS idx_inventory_store_product ON inventory (store_id, product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse_product ON inventory (warehouse_id, product_id);

-- Loja não pode divergir do depósito (integridade)
CREATE OR REPLACE FUNCTION inventory_sync_store_from_warehouse()
RETURNS TRIGGER AS $$
BEGIN
    SELECT store_id INTO NEW.store_id FROM warehouses WHERE id = NEW.warehouse_id;
    IF NEW.store_id IS NULL THEN
        RAISE EXCEPTION 'Depósito % sem loja', NEW.warehouse_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_inventory_sync_store ON inventory;
CREATE TRIGGER trg_inventory_sync_store
    BEFORE INSERT OR UPDATE OF warehouse_id ON inventory
    FOR EACH ROW
    EXECUTE PROCEDURE inventory_sync_store_from_warehouse();

COMMENT ON COLUMN inventory.quantity IS 'Saldo físico (on-hand) no depósito';
COMMENT ON COLUMN inventory.quantity_reserved IS 'Quantidade reservada (não disponível para nova venda)';
COMMENT ON COLUMN inventory.quantity_blocked IS 'Quantidade bloqueada (indisponível comercialmente)';
COMMENT ON COLUMN inventory.quantity_in_transit IS 'Quantidade em trânsito (entrada pendente neste depósito)';
COMMENT ON COLUMN inventory.store_id IS 'Loja derivada do depósito (denormalizada para consulta)';
