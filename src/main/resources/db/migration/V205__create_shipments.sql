-- V205: Expedição e entrega (Prompt 72)
CREATE TABLE store_shipment_sequences (
    store_id    UUID NOT NULL,
    last_value  BIGINT NOT NULL DEFAULT 0,
    prefix      VARCHAR(10) NOT NULL DEFAULT 'XP',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_shipment_seq PRIMARY KEY (store_id),
    CONSTRAINT fk_store_shipment_seq_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE shipments (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    warehouse_id        UUID            NOT NULL,
    sales_order_id      UUID            NOT NULL,
    picking_order_id    UUID            NULL,
    customer_id         UUID            NULL,
    shipment_number     VARCHAR(40)     NOT NULL,
    carrier_name        VARCHAR(200)    NULL,
    freight_mode        VARCHAR(40)     NULL,
    freight_amount      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    tracking_code       VARCHAR(80)     NULL,
    package_count       INT             NOT NULL DEFAULT 1,
    total_weight        NUMERIC(18, 4)  NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    expected_delivery   DATE            NULL,
    address_snapshot    TEXT            NULL,
    responsible_user_id UUID            NULL,
    notes               VARCHAR(2000)   NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_shipments PRIMARY KEY (id),
    CONSTRAINT uk_shipments_number UNIQUE (shipment_number),
    CONSTRAINT fk_sh_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_sh_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_sh_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_sh_so FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id),
    CONSTRAINT fk_sh_picking FOREIGN KEY (picking_order_id) REFERENCES picking_orders (id),
    CONSTRAINT fk_sh_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_sh_status CHECK (status IN (
        'PENDING', 'PACKING', 'READY', 'DISPATCHED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY',
        'DELIVERED', 'DELIVERY_FAILED', 'RETURNING', 'RETURNED', 'CANCELLED'
    ))
);

CREATE TABLE shipment_items (
    id                  UUID            NOT NULL,
    shipment_id         UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    picking_item_id     UUID            NULL,
    sales_order_item_id UUID            NULL,
    line_number         INT             NOT NULL,
    quantity            NUMERIC(18, 4)  NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_shipment_items PRIMARY KEY (id),
    CONSTRAINT uk_shi_line UNIQUE (shipment_id, line_number),
    CONSTRAINT fk_shi_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE,
    CONSTRAINT fk_shi_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_shi_qty CHECK (quantity > 0)
);

CREATE TABLE shipment_packages (
    id                  UUID            NOT NULL,
    shipment_id         UUID            NOT NULL,
    package_number      INT             NOT NULL,
    weight              NUMERIC(18, 4)  NULL,
    length_cm           NUMERIC(18, 4)  NULL,
    width_cm            NUMERIC(18, 4)  NULL,
    height_cm           NUMERIC(18, 4)  NULL,
    tracking_code       VARCHAR(80)     NULL,
    CONSTRAINT pk_shipment_packages PRIMARY KEY (id),
    CONSTRAINT uk_shp UNIQUE (shipment_id, package_number),
    CONSTRAINT fk_shp_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE
);

CREATE TABLE shipment_tracking (
    id                  UUID            NOT NULL,
    shipment_id         UUID            NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    description         VARCHAR(1000)   NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    location_text       VARCHAR(200)    NULL,
    CONSTRAINT pk_shipment_tracking PRIMARY KEY (id),
    CONSTRAINT fk_st_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE
);

CREATE TABLE delivery_events (
    id                  UUID            NOT NULL,
    shipment_id         UUID            NOT NULL,
    event_type          VARCHAR(40)     NOT NULL,
    notes               VARCHAR(1000)   NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    performed_by        UUID            NULL,
    CONSTRAINT pk_delivery_events PRIMARY KEY (id),
    CONSTRAINT fk_de_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE
);

CREATE TABLE delivery_proofs (
    id                  UUID            NOT NULL,
    shipment_id         UUID            NOT NULL,
    proof_type          VARCHAR(40)     NOT NULL DEFAULT 'SIGNATURE',
    storage_ref         VARCHAR(500)    NOT NULL,
    recipient_name      VARCHAR(200)    NULL,
    captured_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_delivery_proofs PRIMARY KEY (id),
    CONSTRAINT fk_dp_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE,
    CONSTRAINT ck_dp_type CHECK (proof_type IN ('SIGNATURE', 'PHOTO', 'DOCUMENT', 'OTHER'))
);

CREATE INDEX idx_shipments_so ON shipments (sales_order_id);
CREATE INDEX idx_shipments_status ON shipments (store_id, status);

-- Migra carrier_name do SO permanece; FK opcional em V206
COMMENT ON TABLE shipments IS 'Expedição — entrega não altera estoque novamente (Prompt 72)';
