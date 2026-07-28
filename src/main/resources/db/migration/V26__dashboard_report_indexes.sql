-- V26: índices compostos para dashboard e relatórios (consultas por período/status)

CREATE INDEX IF NOT EXISTS idx_sales_status_sale_date
    ON sales (status, sale_date);

CREATE INDEX IF NOT EXISTS idx_payments_status_paid_at
    ON payments (status, paid_at);

CREATE INDEX IF NOT EXISTS idx_sale_items_product_id_sale_id
    ON sale_items (product_id, sale_id);

CREATE INDEX IF NOT EXISTS idx_customers_created_at
    ON customers (created_at);

CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at_type
    ON stock_movements (created_at, type);

COMMENT ON INDEX idx_sales_status_sale_date IS 'Agregações de vendas por status e período';
COMMENT ON INDEX idx_payments_status_paid_at IS 'Recebimentos confirmados por período';
