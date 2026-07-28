-- V169: entradas de estoque por loja/depósito

CREATE TABLE IF NOT EXISTS stock_entries (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    store_id UUID NOT NULL REFERENCES stores (id),
    warehouse_id UUID NOT NULL REFERENCES warehouses (id),
    number VARCHAR(40) NOT NULL,
    supplier_name VARCHAR(200),
    document_number VARCHAR(80),
    entry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    responsible_user_id UUID REFERENCES users (id),
    notes VARCHAR(2000),
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_entries_org_number UNIQUE (organization_id, number),
    CONSTRAINT ck_stock_entries_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS stock_entry_items (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES stock_entries (id),
    product_id UUID NOT NULL REFERENCES products (id),
    quantity NUMERIC(19, 3) NOT NULL,
    unit_cost NUMERIC(19, 4) NOT NULL DEFAULT 0,
    line_total NUMERIC(19, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_entry_items_product UNIQUE (entry_id, product_id),
    CONSTRAINT ck_stock_entry_items_qty_positive CHECK (quantity > 0),
    CONSTRAINT ck_stock_entry_items_unit_cost_nonneg CHECK (unit_cost >= 0)
);

CREATE INDEX IF NOT EXISTS idx_stock_entries_store ON stock_entries (store_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_entries_warehouse ON stock_entries (warehouse_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_entry_items_entry ON stock_entry_items (entry_id);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000132', 'STOCK_ENTRY_READ', 'Consultar entradas de estoque', 'STOCK_ENTRY',
     'Consultar notas de entrada de estoque', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000133', 'STOCK_ENTRY_MANAGE', 'Gerenciar entradas de estoque', 'STOCK_ENTRY',
     'Criar, confirmar e cancelar entradas de estoque', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('STOCK_ENTRY_READ', 'STOCK_ENTRY_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

COMMENT ON TABLE stock_entries IS 'Entrada oficial de estoque (compra/recebimento) por loja e depósito';
