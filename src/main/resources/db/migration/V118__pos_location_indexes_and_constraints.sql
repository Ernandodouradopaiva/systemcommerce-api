-- V118: índices e constraints complementares de localização PDV
-- Garante no máximo um depósito DEFAULT por loja via código DEP-01 já unique (store_id, code).
-- Índice composto para listar terminais disponíveis ao PDV.
CREATE INDEX IF NOT EXISTS idx_pos_terminals_available
    ON pos_terminals (store_id, status, active)
    WHERE status = 'ACTIVE' AND active = TRUE;

CREATE INDEX IF NOT EXISTS idx_warehouses_store_sale
    ON warehouses (store_id, status, allows_sale)
    WHERE status = 'ACTIVE' AND allows_sale = TRUE AND active = TRUE;

-- Relacionamento lógico: depósito deve pertencer à mesma loja do terminal (validado na API;
-- trigger defensiva no banco).
CREATE OR REPLACE FUNCTION trg_pos_terminal_same_store_warehouse()
RETURNS TRIGGER AS $$
DECLARE
    wh_store UUID;
BEGIN
    SELECT store_id INTO wh_store FROM warehouses WHERE id = NEW.warehouse_id;
    IF wh_store IS NULL THEN
        RAISE EXCEPTION 'Depósito % não encontrado', NEW.warehouse_id;
    END IF;
    IF wh_store <> NEW.store_id THEN
        RAISE EXCEPTION 'Depósito do terminal deve pertencer à mesma loja';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pos_terminals_warehouse_store ON pos_terminals;
CREATE TRIGGER trg_pos_terminals_warehouse_store
    BEFORE INSERT OR UPDATE OF store_id, warehouse_id ON pos_terminals
    FOR EACH ROW
    EXECUTE PROCEDURE trg_pos_terminal_same_store_warehouse();
