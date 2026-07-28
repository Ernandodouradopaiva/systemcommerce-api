-- V163: tipos de movimentação para transferência de estoque

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
        'TRANSFER_IN_TRANSIT'
    ));
