-- V213: BOM e ordens de produção — preparação MRP (Prompt 79)
CREATE TABLE bills_of_materials (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    finished_product_id     UUID            NOT NULL,
    code                    VARCHAR(40)     NOT NULL,
    name                    VARCHAR(200)    NOT NULL,
    version_number          INT             NOT NULL DEFAULT 1,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    notes                   VARCHAR(2000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_bills_of_materials PRIMARY KEY (id),
    CONSTRAINT uk_bom_code_ver UNIQUE (organization_id, code, version_number),
    CONSTRAINT fk_bom_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_bom_product FOREIGN KEY (finished_product_id) REFERENCES products (id),
    CONSTRAINT ck_bom_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'SUPERSEDED'))
);

CREATE TABLE bill_of_materials_items (
    id                      UUID            NOT NULL,
    bill_of_materials_id    UUID            NOT NULL,
    component_product_id    UUID            NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    unit_code               VARCHAR(20)     NULL,
    scrap_percent           NUMERIC(7, 4)   NOT NULL DEFAULT 0,
    line_number             INT             NOT NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_bom_items PRIMARY KEY (id),
    CONSTRAINT uk_bom_items_line UNIQUE (bill_of_materials_id, line_number),
    CONSTRAINT fk_bomi_bom FOREIGN KEY (bill_of_materials_id) REFERENCES bills_of_materials (id) ON DELETE CASCADE,
    CONSTRAINT fk_bomi_component FOREIGN KEY (component_product_id) REFERENCES products (id),
    CONSTRAINT ck_bomi_qty CHECK (quantity > 0 AND scrap_percent >= 0)
);

CREATE TABLE production_orders (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    bill_of_materials_id    UUID            NOT NULL,
    finished_product_id     UUID            NOT NULL,
    order_number            VARCHAR(40)     NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    quantity_planned        NUMERIC(18, 4)  NOT NULL,
    quantity_completed      NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    planned_start           TIMESTAMPTZ     NULL,
    planned_end             TIMESTAMPTZ     NULL,
    started_at              TIMESTAMPTZ     NULL,
    completed_at            TIMESTAMPTZ     NULL,
    unit_cost               NUMERIC(18, 4)  NULL,
    total_cost              NUMERIC(18, 4)  NULL,
    notes                   VARCHAR(2000)   NULL,
    idempotency_key         VARCHAR(80)     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_production_orders PRIMARY KEY (id),
    CONSTRAINT uk_po_number UNIQUE (order_number),
    CONSTRAINT fk_po_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_po_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_po_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_po_bom FOREIGN KEY (bill_of_materials_id) REFERENCES bills_of_materials (id),
    CONSTRAINT fk_po_product FOREIGN KEY (finished_product_id) REFERENCES products (id),
    CONSTRAINT ck_po_status CHECK (status IN (
        'DRAFT', 'PLANNED', 'RELEASED', 'IN_PROGRESS',
        'PARTIALLY_COMPLETED', 'COMPLETED', 'CANCELLED'
    )),
    CONSTRAINT ck_po_qty CHECK (quantity_planned > 0 AND quantity_completed >= 0)
);

CREATE UNIQUE INDEX uk_po_idempotency ON production_orders (organization_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE production_consumptions (
    id                      UUID            NOT NULL,
    production_order_id     UUID            NOT NULL,
    component_product_id    UUID            NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    inventory_movement_id   UUID            NULL,
    unit_cost               NUMERIC(18, 4)  NULL,
    consumed_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_production_consumptions PRIMARY KEY (id),
    CONSTRAINT fk_pc_order FOREIGN KEY (production_order_id) REFERENCES production_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pc_product FOREIGN KEY (component_product_id) REFERENCES products (id),
    CONSTRAINT fk_pc_movement FOREIGN KEY (inventory_movement_id) REFERENCES stock_movements (id),
    CONSTRAINT ck_pc_qty CHECK (quantity > 0)
);

CREATE TABLE production_outputs (
    id                      UUID            NOT NULL,
    production_order_id     UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    inventory_movement_id   UUID            NULL,
    unit_cost               NUMERIC(18, 4)  NULL,
    produced_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_production_outputs PRIMARY KEY (id),
    CONSTRAINT fk_pout_order FOREIGN KEY (production_order_id) REFERENCES production_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pout_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_pout_movement FOREIGN KEY (inventory_movement_id) REFERENCES stock_movements (id),
    CONSTRAINT ck_pout_qty CHECK (quantity > 0)
);

CREATE TABLE production_losses (
    id                      UUID            NOT NULL,
    production_order_id     UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    reason                  VARCHAR(200)    NULL,
    inventory_movement_id   UUID            NULL,
    recorded_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_production_losses PRIMARY KEY (id),
    CONSTRAINT fk_pl_order FOREIGN KEY (production_order_id) REFERENCES production_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pl_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_pl_movement FOREIGN KEY (inventory_movement_id) REFERENCES stock_movements (id),
    CONSTRAINT ck_pl_qty CHECK (quantity > 0)
);

CREATE TABLE production_order_status_history (
    id                      UUID            NOT NULL,
    production_order_id     UUID            NOT NULL,
    from_status             VARCHAR(30)     NULL,
    to_status               VARCHAR(30)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_posh PRIMARY KEY (id),
    CONSTRAINT fk_posh_order FOREIGN KEY (production_order_id) REFERENCES production_orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_po_store_status ON production_orders (store_id, status);

COMMENT ON TABLE production_orders IS 'Ordem de produção — consumo OUT + acabado IN via movimentos (Prompt 79)';
