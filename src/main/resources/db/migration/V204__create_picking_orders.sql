-- V204: Separação / Picking (Prompt 71)
CREATE TABLE store_picking_order_sequences (
    store_id    UUID NOT NULL,
    last_value  BIGINT NOT NULL DEFAULT 0,
    prefix      VARCHAR(10) NOT NULL DEFAULT 'SP',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_picking_seq PRIMARY KEY (store_id),
    CONSTRAINT fk_store_picking_seq_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE picking_orders (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    warehouse_id        UUID            NOT NULL,
    sales_order_id      UUID            NOT NULL,
    stock_reservation_id UUID           NULL,
    picking_number      VARCHAR(40)     NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    assigned_to_user_id UUID            NULL,
    started_at          TIMESTAMPTZ     NULL,
    completed_at        TIMESTAMPTZ     NULL,
    notes               VARCHAR(2000)   NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_picking_orders PRIMARY KEY (id),
    CONSTRAINT uk_picking_orders_number UNIQUE (picking_number),
    CONSTRAINT fk_po_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_po_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_po_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_po_so FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id),
    CONSTRAINT fk_po_reservation FOREIGN KEY (stock_reservation_id) REFERENCES stock_reservations (id),
    CONSTRAINT fk_po_assignee FOREIGN KEY (assigned_to_user_id) REFERENCES users (id),
    CONSTRAINT ck_picking_status CHECK (status IN (
        'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'PARTIALLY_PICKED', 'PICKED', 'DIVERGENT', 'CANCELLED'
    ))
);

CREATE TABLE picking_order_items (
    id                      UUID            NOT NULL,
    picking_order_id        UUID            NOT NULL,
    sales_order_item_id     UUID            NULL,
    product_id              UUID            NOT NULL,
    storage_location_id     UUID            NULL,
    line_number             INT             NOT NULL,
    quantity_requested      NUMERIC(18, 4)  NOT NULL,
    quantity_picked         NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    barcode_scanned         VARCHAR(80)     NULL,
    substitute_product_id   UUID            NULL,
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_picking_order_items PRIMARY KEY (id),
    CONSTRAINT uk_poi_line UNIQUE (picking_order_id, line_number),
    CONSTRAINT fk_poi_order FOREIGN KEY (picking_order_id) REFERENCES picking_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_poi_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_poi_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT fk_poi_substitute FOREIGN KEY (substitute_product_id) REFERENCES products (id),
    CONSTRAINT ck_poi_qty CHECK (quantity_requested > 0 AND quantity_picked >= 0)
);

CREATE TABLE picking_assignments (
    id                  UUID            NOT NULL,
    picking_order_id    UUID            NOT NULL,
    user_id             UUID            NOT NULL,
    assigned_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    released_at         TIMESTAMPTZ     NULL,
    CONSTRAINT pk_picking_assignments PRIMARY KEY (id),
    CONSTRAINT fk_pa_order FOREIGN KEY (picking_order_id) REFERENCES picking_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pa_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE picking_events (
    id                  UUID            NOT NULL,
    picking_order_id    UUID            NOT NULL,
    picking_item_id     UUID            NULL,
    event_type          VARCHAR(40)     NOT NULL,
    quantity            NUMERIC(18, 4)  NULL,
    barcode             VARCHAR(80)     NULL,
    notes               VARCHAR(1000)   NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    performed_by        UUID            NULL,
    idempotency_key     VARCHAR(80)     NULL,
    CONSTRAINT pk_picking_events PRIMARY KEY (id),
    CONSTRAINT fk_pe_order FOREIGN KEY (picking_order_id) REFERENCES picking_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pe_item FOREIGN KEY (picking_item_id) REFERENCES picking_order_items (id)
);

CREATE UNIQUE INDEX uk_picking_events_idem ON picking_events (picking_order_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE picking_divergences (
    id                  UUID            NOT NULL,
    picking_order_id    UUID            NOT NULL,
    picking_item_id     UUID            NULL,
    divergence_type     VARCHAR(40)     NOT NULL,
    description         VARCHAR(1000)   NOT NULL,
    quantity            NUMERIC(18, 4)  NULL,
    resolved            BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    CONSTRAINT pk_picking_divergences PRIMARY KEY (id),
    CONSTRAINT fk_pd_order FOREIGN KEY (picking_order_id) REFERENCES picking_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_pd_item FOREIGN KEY (picking_item_id) REFERENCES picking_order_items (id),
    CONSTRAINT ck_pd_type CHECK (divergence_type IN (
        'SHORTAGE', 'DAMAGE', 'WRONG_PRODUCT', 'LOCATION', 'OTHER'
    ))
);

CREATE INDEX idx_picking_orders_so ON picking_orders (sales_order_id);
CREATE INDEX idx_picking_orders_status ON picking_orders (store_id, status);
