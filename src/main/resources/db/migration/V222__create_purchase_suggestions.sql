-- V222: Sugestão de compras determinística (Prompt 89)

CREATE TABLE purchase_suggestion_parameters (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NULL,
    product_id              UUID            NULL,
    default_lead_time_days  INT             NOT NULL DEFAULT 7,
    safety_stock_days       NUMERIC(8, 2)   NOT NULL DEFAULT 3,
    seasonality_factor      NUMERIC(8, 4)   NOT NULL DEFAULT 1.0000,
    min_purchase_multiple   NUMERIC(18, 4)  NOT NULL DEFAULT 1,
    min_lot_size            NUMERIC(18, 4)  NOT NULL DEFAULT 1,
    coverage_target_days    NUMERIC(8, 2)   NOT NULL DEFAULT 14,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_suggestion_parameters PRIMARY KEY (id),
    CONSTRAINT fk_psp_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_psp_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_psp_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE UNIQUE INDEX uk_psp_org_store_product ON purchase_suggestion_parameters (organization_id, store_id, product_id)
    WHERE store_id IS NOT NULL AND product_id IS NOT NULL;

CREATE TABLE purchase_suggestion_executions (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NULL,
    warehouse_id            UUID            NULL,
    execution_type          VARCHAR(30)     NOT NULL DEFAULT 'FULL',
    status                  VARCHAR(30)     NOT NULL DEFAULT 'COMPLETED',
    items_generated         INT             NOT NULL DEFAULT 0,
    parameters_snapshot     TEXT            NULL,
    started_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    finished_at             TIMESTAMPTZ     NULL,
    created_by              UUID            NULL,
    CONSTRAINT pk_purchase_suggestion_executions PRIMARY KEY (id),
    CONSTRAINT fk_pse_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pse_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_pse_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_pse_type CHECK (execution_type IN ('FULL', 'SIMULATION')),
    CONSTRAINT ck_pse_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE purchase_suggestions (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    execution_id            UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    supplier_id             UUID            NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    total_items             INT             NOT NULL DEFAULT 0,
    total_suggested_qty     NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_suggestions PRIMARY KEY (id),
    CONSTRAINT fk_ps_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ps_exec FOREIGN KEY (execution_id) REFERENCES purchase_suggestion_executions (id),
    CONSTRAINT fk_ps_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_ps_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_ps_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT ck_ps_status CHECK (status IN ('DRAFT', 'REVIEWED', 'CONVERTED', 'DISCARDED'))
);

CREATE TABLE purchase_suggestion_items (
    id                      UUID            NOT NULL,
    suggestion_id           UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    supplier_id             UUID            NULL,
    on_hand_qty             NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    available_qty           NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    in_transit_qty          NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    open_po_qty             NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    avg_daily_consumption   NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    coverage_days           NUMERIC(18, 4)  NULL,
    reorder_point           NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    max_stock               NUMERIC(18, 4)  NULL,
    suggested_qty           NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    confidence_level        NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    justification           VARCHAR(2000)   NOT NULL,
    parameters_used_json    TEXT            NULL,
    line_number             INT             NOT NULL,
    CONSTRAINT pk_purchase_suggestion_items PRIMARY KEY (id),
    CONSTRAINT uk_psi_line UNIQUE (suggestion_id, line_number),
    CONSTRAINT fk_psi_suggestion FOREIGN KEY (suggestion_id) REFERENCES purchase_suggestions (id) ON DELETE CASCADE,
    CONSTRAINT fk_psi_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_psi_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

CREATE INDEX idx_ps_exec ON purchase_suggestions (execution_id);
CREATE INDEX idx_psi_product ON purchase_suggestion_items (product_id);

COMMENT ON TABLE purchase_suggestions IS 'Sugestão determinística (Prompt 89) — não cria PO automaticamente';
