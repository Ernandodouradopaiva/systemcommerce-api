-- V208: Inventário físico e rotativo (Prompt 74)
CREATE TABLE inventory_counts (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    count_number            VARCHAR(40)     NOT NULL,
    count_type              VARCHAR(30)     NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PLANNED',
    freeze_balances         BOOLEAN         NOT NULL DEFAULT FALSE,
    hide_theoretical_qty    BOOLEAN         NOT NULL DEFAULT TRUE,
    require_second_count    BOOLEAN         NOT NULL DEFAULT FALSE,
    category_id             UUID            NULL,
    brand_id                UUID            NULL,
    storage_location_id     UUID            NULL,
    planned_at              TIMESTAMPTZ     NULL,
    opened_at               TIMESTAMPTZ     NULL,
    closed_at               TIMESTAMPTZ     NULL,
    posted_at               TIMESTAMPTZ     NULL,
    notes                   VARCHAR(2000)   NULL,
    idempotency_key         VARCHAR(80)     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory_counts PRIMARY KEY (id),
    CONSTRAINT uk_inventory_counts_number UNIQUE (count_number),
    CONSTRAINT fk_ic_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ic_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_ic_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_ic_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_ic_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT fk_ic_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id),
    CONSTRAINT ck_ic_type CHECK (count_type IN (
        'GENERAL', 'WAREHOUSE', 'LOCATION', 'CATEGORY', 'BRAND', 'CYCLE', 'SAMPLING'
    )),
    CONSTRAINT ck_ic_status CHECK (status IN (
        'PLANNED', 'OPEN', 'COUNTING', 'RECOUNTING', 'UNDER_ANALYSIS',
        'APPROVED', 'POSTED', 'CANCELLED'
    ))
);

CREATE UNIQUE INDEX uk_ic_idempotency ON inventory_counts (organization_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE inventory_count_sessions (
    id                      UUID            NOT NULL,
    inventory_count_id      UUID            NOT NULL,
    session_number          INT             NOT NULL,
    counter_user_id         UUID            NULL,
    started_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    finished_at             TIMESTAMPTZ     NULL,
    notes                   VARCHAR(1000)   NULL,
    CONSTRAINT pk_inventory_count_sessions PRIMARY KEY (id),
    CONSTRAINT uk_ics UNIQUE (inventory_count_id, session_number),
    CONSTRAINT fk_ics_count FOREIGN KEY (inventory_count_id) REFERENCES inventory_counts (id) ON DELETE CASCADE,
    CONSTRAINT fk_ics_user FOREIGN KEY (counter_user_id) REFERENCES users (id)
);

CREATE TABLE inventory_count_items (
    id                      UUID            NOT NULL,
    inventory_count_id      UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    storage_location_id     UUID            NULL,
    line_number             INT             NOT NULL,
    theoretical_quantity    NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    counted_quantity_1      NUMERIC(18, 4)  NULL,
    counted_quantity_2      NUMERIC(18, 4)  NULL,
    final_counted_quantity  NUMERIC(18, 4)  NULL,
    variance_quantity       NUMERIC(18, 4)  NULL,
    unit_cost               NUMERIC(18, 4)  NULL,
    frozen                  BOOLEAN         NOT NULL DEFAULT FALSE,
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory_count_items PRIMARY KEY (id),
    CONSTRAINT uk_ici_line UNIQUE (inventory_count_id, line_number),
    CONSTRAINT uk_ici_product_loc UNIQUE (inventory_count_id, product_id, storage_location_id),
    CONSTRAINT fk_ici_count FOREIGN KEY (inventory_count_id) REFERENCES inventory_counts (id) ON DELETE CASCADE,
    CONSTRAINT fk_ici_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_ici_location FOREIGN KEY (storage_location_id) REFERENCES storage_locations (id)
);

CREATE TABLE inventory_count_entries (
    id                      UUID            NOT NULL,
    inventory_count_id      UUID            NOT NULL,
    inventory_count_item_id UUID            NOT NULL,
    session_id              UUID            NULL,
    count_pass              INT             NOT NULL DEFAULT 1,
    quantity                NUMERIC(18, 4)  NOT NULL,
    barcode                 VARCHAR(80)     NULL,
    entered_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    entered_by              UUID            NULL,
    idempotency_key         VARCHAR(80)     NULL,
    CONSTRAINT pk_inventory_count_entries PRIMARY KEY (id),
    CONSTRAINT fk_ice_count FOREIGN KEY (inventory_count_id) REFERENCES inventory_counts (id) ON DELETE CASCADE,
    CONSTRAINT fk_ice_item FOREIGN KEY (inventory_count_item_id) REFERENCES inventory_count_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_ice_session FOREIGN KEY (session_id) REFERENCES inventory_count_sessions (id),
    CONSTRAINT ck_ice_pass CHECK (count_pass IN (1, 2)),
    CONSTRAINT ck_ice_qty CHECK (quantity >= 0)
);

CREATE UNIQUE INDEX uk_ice_idempotency ON inventory_count_entries (inventory_count_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE inventory_count_adjustments (
    id                      UUID            NOT NULL,
    inventory_count_id      UUID            NOT NULL,
    inventory_count_item_id UUID            NOT NULL,
    inventory_movement_id   UUID            NULL,
    product_id              UUID            NOT NULL,
    variance_quantity       NUMERIC(18, 4)  NOT NULL,
    posted_at               TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    posted_by               UUID            NULL,
    CONSTRAINT pk_inventory_count_adjustments PRIMARY KEY (id),
    CONSTRAINT fk_ica_count FOREIGN KEY (inventory_count_id) REFERENCES inventory_counts (id),
    CONSTRAINT fk_ica_item FOREIGN KEY (inventory_count_item_id) REFERENCES inventory_count_items (id),
    CONSTRAINT fk_ica_movement FOREIGN KEY (inventory_movement_id) REFERENCES stock_movements (id),
    CONSTRAINT fk_ica_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE inventory_count_status_history (
    id                      UUID            NOT NULL,
    inventory_count_id      UUID            NOT NULL,
    from_status             VARCHAR(30)     NULL,
    to_status               VARCHAR(30)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_ic_status_history PRIMARY KEY (id),
    CONSTRAINT fk_icsh_count FOREIGN KEY (inventory_count_id) REFERENCES inventory_counts (id) ON DELETE CASCADE
);

CREATE INDEX idx_ic_store_status ON inventory_counts (store_id, status);
CREATE INDEX idx_ici_count ON inventory_count_items (inventory_count_id);

COMMENT ON TABLE inventory_counts IS 'Inventário físico/rotativo (Prompt 74); postagem gera ajustes via movimentos';
