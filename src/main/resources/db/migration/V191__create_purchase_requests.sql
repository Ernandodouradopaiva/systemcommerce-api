-- V191: Solicitação interna de compra (Prompt 59)
CREATE TABLE store_purchase_request_sequences (
    store_id    UUID            NOT NULL,
    last_value  BIGINT          NOT NULL DEFAULT 0,
    prefix      VARCHAR(10)     NOT NULL DEFAULT 'SC',
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_purchase_request_sequences PRIMARY KEY (store_id),
    CONSTRAINT fk_store_pr_seq_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE purchase_requests (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    warehouse_id            UUID            NULL,
    requesting_sector       VARCHAR(120)    NULL,
    requester_user_id       UUID            NULL,
    buyer_user_id           UUID            NULL,
    request_number          VARCHAR(40)     NOT NULL,
    priority                VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    requested_at            TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    desired_date            DATE            NULL,
    justification           VARCHAR(2000)   NULL,
    notes                   VARCHAR(2000)   NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    requires_approval       BOOLEAN         NOT NULL DEFAULT TRUE,
    rejection_reason        VARCHAR(1000)   NULL,
    cancellation_reason     VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_requests PRIMARY KEY (id),
    CONSTRAINT uk_purchase_requests_number UNIQUE (request_number),
    CONSTRAINT fk_purchase_requests_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_purchase_requests_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_purchase_requests_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_purchase_requests_requester FOREIGN KEY (requester_user_id) REFERENCES users (id),
    CONSTRAINT fk_purchase_requests_buyer FOREIGN KEY (buyer_user_id) REFERENCES users (id),
    CONSTRAINT ck_purchase_requests_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_purchase_requests_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'UNDER_ANALYSIS', 'APPROVED', 'PARTIALLY_APPROVED',
        'REJECTED', 'IN_QUOTATION', 'CONVERTED', 'CANCELLED'
    ))
);

CREATE INDEX idx_purchase_requests_store_status ON purchase_requests (store_id, status);
CREATE INDEX idx_purchase_requests_org ON purchase_requests (organization_id);

CREATE TABLE purchase_request_items (
    id                          UUID            NOT NULL,
    purchase_request_id         UUID            NOT NULL,
    product_id                  UUID            NULL,
    line_number                 INT             NOT NULL,
    description                 VARCHAR(300)    NOT NULL,
    quantity_requested          NUMERIC(18, 4)  NOT NULL,
    quantity_approved           NUMERIC(18, 4)  NULL,
    quantity_converted          NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    unit                        VARCHAR(30)     NULL,
    current_stock_info          NUMERIC(18, 4)  NULL,
    minimum_stock               NUMERIC(18, 4)  NULL,
    justification               VARCHAR(1000)   NULL,
    suggested_supplier_id       UUID            NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_request_items PRIMARY KEY (id),
    CONSTRAINT uk_purchase_request_items_line UNIQUE (purchase_request_id, line_number),
    CONSTRAINT fk_pr_items_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_pr_items_supplier FOREIGN KEY (suggested_supplier_id) REFERENCES suppliers (id),
    CONSTRAINT ck_pr_items_qty CHECK (quantity_requested > 0 AND quantity_converted >= 0)
);

CREATE TABLE purchase_request_status_history (
    id                      UUID            NOT NULL,
    purchase_request_id     UUID            NOT NULL,
    from_status             VARCHAR(30)     NULL,
    to_status               VARCHAR(30)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_purchase_request_status_history PRIMARY KEY (id),
    CONSTRAINT fk_pr_status_hist_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_status_hist_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_pr_status_hist_request ON purchase_request_status_history (purchase_request_id, changed_at);

COMMENT ON TABLE purchase_requests IS 'Solicitação interna de compra — não altera estoque nem financeiro (Prompt 59)';
