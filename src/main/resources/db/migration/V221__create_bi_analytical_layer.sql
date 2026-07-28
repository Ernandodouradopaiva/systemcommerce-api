-- V221: Camada analítica / BI (Prompt 88)
-- Views materializadas para relatórios complexos; transacional permanece fonte oficial.

CREATE TABLE bi_refresh_log (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    object_name             VARCHAR(120)    NOT NULL,
    refresh_type            VARCHAR(30)     NOT NULL DEFAULT 'MATERIALIZED_VIEW',
    started_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    finished_at             TIMESTAMPTZ     NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'RUNNING',
    rows_affected           BIGINT          NULL,
    error_message           VARCHAR(2000)   NULL,
    CONSTRAINT pk_bi_refresh_log PRIMARY KEY (id),
    CONSTRAINT ck_bi_refresh_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_bi_refresh_object ON bi_refresh_log (object_name, started_at DESC);

-- Fato: vendas diárias por loja (dimensões: tempo, loja)
CREATE MATERIALIZED VIEW IF NOT EXISTS bi_fact_sales_daily AS
SELECT
    s.organization_id,
    s.store_id,
    (s.sale_date AT TIME ZONE 'UTC')::date AS sale_day,
    COUNT(*)::bigint AS order_count,
    COALESCE(SUM(s.total_amount), 0) AS revenue,
    COALESCE(SUM(si.quantity * COALESCE(p.cost_price, 0)), 0) AS cost_amount
FROM sales s
JOIN sale_items si ON si.sale_id = s.id
JOIN products p ON p.id = si.product_id
WHERE s.status IN ('CONFIRMED', 'PAID', 'PARTIALLY_PAID')
GROUP BY s.organization_id, s.store_id, (s.sale_date AT TIME ZONE 'UTC')::date
WITH NO DATA;

CREATE UNIQUE INDEX uk_bi_fact_sales_daily ON bi_fact_sales_daily (organization_id, store_id, sale_day);

-- Fato: estoque atual por produto/depósito/loja
CREATE MATERIALIZED VIEW IF NOT EXISTS bi_fact_inventory_snapshot AS
SELECT
    p.organization_id,
    i.store_id,
    i.warehouse_id,
    i.product_id,
    p.category_id,
    p.brand_id,
    i.quantity AS on_hand,
    i.quantity_reserved,
    i.quantity_blocked,
    i.quantity_in_transit,
    GREATEST(i.quantity - i.quantity_reserved - i.quantity_blocked, 0) AS available,
    COALESCE(i.minimum_quantity, p.min_stock, 0) AS min_qty,
    i.reorder_point,
    i.maximum_quantity,
    COALESCE(p.cost_price, 0) AS unit_cost,
    (i.quantity * COALESCE(p.cost_price, 0)) AS stock_value
FROM inventory i
JOIN products p ON p.id = i.product_id
WITH NO DATA;

CREATE UNIQUE INDEX uk_bi_fact_inventory ON bi_fact_inventory_snapshot (warehouse_id, product_id);

-- Fato: compras diárias
CREATE MATERIALIZED VIEW IF NOT EXISTS bi_fact_purchases_daily AS
SELECT
    po.organization_id,
    po.destination_store_id AS store_id,
    (po.created_at AT TIME ZONE 'UTC')::date AS purchase_day,
    COUNT(*)::bigint AS po_count,
    COALESCE(SUM(po.total_amount), 0) AS purchase_amount
FROM purchase_orders po
WHERE po.status NOT IN ('CANCELLED', 'REJECTED', 'DRAFT')
GROUP BY po.organization_id, po.destination_store_id, (po.created_at AT TIME ZONE 'UTC')::date
WITH NO DATA;

CREATE UNIQUE INDEX uk_bi_fact_purchases_daily ON bi_fact_purchases_daily (organization_id, store_id, purchase_day);

COMMENT ON MATERIALIZED VIEW bi_fact_sales_daily IS 'BI Prompt 88 — vendas diárias; refresh via BiRefreshService';
COMMENT ON MATERIALIZED VIEW bi_fact_inventory_snapshot IS 'BI Prompt 88 — snapshot estoque';
COMMENT ON MATERIALIZED VIEW bi_fact_purchases_daily IS 'BI Prompt 88 — compras diárias';
