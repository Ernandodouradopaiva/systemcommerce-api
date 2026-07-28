-- V179: tipos de movimentação de estoque ampliados (Prompt 62)
-- O saldo NUNCA é alterado sem registro em stock_movements.

ALTER TABLE stock_movements
    DROP CONSTRAINT IF EXISTS ck_stock_movements_type;

ALTER TABLE stock_movements
    ADD CONSTRAINT ck_stock_movements_type CHECK (type IN (
        'ENTRY',
        'EXIT',
        'ADJUSTMENT_POSITIVE',
        'ADJUSTMENT_NEGATIVE',
        'SALE',
        'SALE_CANCEL',
        'FUTURE_RETURN',
        'CORRECTION',
        'TRANSFER_OUT',
        'TRANSFER_IN',
        'TRANSFER_IN_TRANSIT',
        'PURCHASE',
        'PURCHASE_CANCEL',
        'CUSTOMER_RETURN',
        'SUPPLIER_RETURN',
        'INVENTORY',
        'PRODUCTION',
        'INTERNAL_CONSUMPTION'
    ));

COMMENT ON COLUMN stock_movements.type IS
    'Tipos: ENTRY/EXIT legados; PURCHASE/VENDA/TRANSFER/AJUSTES/DEVOLUÇÕES/INVENTÁRIO/CANCELAMENTOS/CONSUMO (Prompt 62)';
