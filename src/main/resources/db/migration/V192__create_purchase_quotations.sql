-- V192: Cotação de compras com múltiplos fornecedores (Prompt 60)
-- document_conversions já usa SUPPLIER_QUOTATION; tabela canônica: purchase_quotations

CREATE TABLE store_purchase_quotation_sequences (
    store_id    UUID            NOT NULL,
    last_value  BIGINT          NOT NULL DEFAULT 0,
    prefix      VARCHAR(10)     NOT NULL DEFAULT 'CC',
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_store_purchase_quotation_sequences PRIMARY KEY (store_id),
    CONSTRAINT fk_store_pq_seq_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE purchase_quotations (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    store_id                    UUID            NOT NULL,
    buyer_user_id               UUID            NULL,
    purchase_request_id         UUID            NULL,
    quotation_number            VARCHAR(40)     NOT NULL,
    opened_at                   TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    response_deadline           TIMESTAMPTZ     NULL,
    status                      VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    selection_criteria          VARCHAR(40)     NOT NULL DEFAULT 'TOTAL_COST',
    auto_select_lowest_price    BOOLEAN         NOT NULL DEFAULT FALSE,
    notes                       VARCHAR(2000)   NULL,
    closed_at                   TIMESTAMPTZ     NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_quotations PRIMARY KEY (id),
    CONSTRAINT uk_purchase_quotations_number UNIQUE (quotation_number),
    CONSTRAINT fk_pq_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pq_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_pq_buyer FOREIGN KEY (buyer_user_id) REFERENCES users (id),
    CONSTRAINT fk_pq_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests (id),
    CONSTRAINT ck_pq_status CHECK (status IN (
        'DRAFT', 'OPEN', 'SENT', 'RESPONSES_PENDING', 'UNDER_COMPARISON',
        'PARTIALLY_SELECTED', 'SELECTED', 'CLOSED', 'CANCELLED'
    )),
    CONSTRAINT ck_pq_criteria CHECK (selection_criteria IN (
        'TOTAL_COST', 'UNIT_PRICE', 'LEAD_TIME', 'MANUAL'
    ))
);

CREATE INDEX idx_pq_store_status ON purchase_quotations (store_id, status);
CREATE INDEX idx_pq_request ON purchase_quotations (purchase_request_id);

CREATE TABLE purchase_quotation_items (
    id                          UUID            NOT NULL,
    purchase_quotation_id       UUID            NOT NULL,
    purchase_request_item_id    UUID            NULL,
    product_id                  UUID            NULL,
    line_number                 INT             NOT NULL,
    description                 VARCHAR(300)    NOT NULL,
    quantity                    NUMERIC(18, 4)  NOT NULL,
    unit                        VARCHAR(30)     NULL,
    quantity_selected           NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_quotation_items PRIMARY KEY (id),
    CONSTRAINT uk_pq_items_line UNIQUE (purchase_quotation_id, line_number),
    CONSTRAINT fk_pq_items_quotation FOREIGN KEY (purchase_quotation_id) REFERENCES purchase_quotations (id) ON DELETE CASCADE,
    CONSTRAINT fk_pq_items_pr_item FOREIGN KEY (purchase_request_item_id) REFERENCES purchase_request_items (id),
    CONSTRAINT fk_pq_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_pq_items_qty CHECK (quantity > 0 AND quantity_selected >= 0)
);

CREATE TABLE purchase_quotation_suppliers (
    id                          UUID            NOT NULL,
    purchase_quotation_id       UUID            NOT NULL,
    supplier_id                 UUID            NOT NULL,
    invited_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    status                      VARCHAR(30)     NOT NULL DEFAULT 'INVITED',
    notes                       VARCHAR(1000)   NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_quotation_suppliers PRIMARY KEY (id),
    CONSTRAINT uk_pq_suppliers UNIQUE (purchase_quotation_id, supplier_id),
    CONSTRAINT fk_pq_sup_quotation FOREIGN KEY (purchase_quotation_id) REFERENCES purchase_quotations (id) ON DELETE CASCADE,
    CONSTRAINT fk_pq_sup_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT ck_pq_sup_status CHECK (status IN ('INVITED', 'RESPONDED', 'DECLINED', 'SELECTED', 'NOT_SELECTED'))
);

CREATE TABLE supplier_quotation_responses (
    id                              UUID            NOT NULL,
    purchase_quotation_id           UUID            NOT NULL,
    purchase_quotation_supplier_id  UUID            NOT NULL,
    supplier_id                     UUID            NOT NULL,
    payment_condition               VARCHAR(200)    NULL,
    freight_amount                  NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    tax_amount                      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount                 NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    lead_time_days                  INT             NULL,
    valid_until                     DATE            NULL,
    notes                           VARCHAR(2000)   NULL,
    total_amount                    NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    submitted_at                    TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    locked                          BOOLEAN         NOT NULL DEFAULT FALSE,
    active                          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                      UUID            NULL,
    updated_by                      UUID            NULL,
    version                         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_quotation_responses PRIMARY KEY (id),
    CONSTRAINT uk_sqr_supplier UNIQUE (purchase_quotation_id, supplier_id),
    CONSTRAINT fk_sqr_quotation FOREIGN KEY (purchase_quotation_id) REFERENCES purchase_quotations (id) ON DELETE CASCADE,
    CONSTRAINT fk_sqr_pq_supplier FOREIGN KEY (purchase_quotation_supplier_id) REFERENCES purchase_quotation_suppliers (id),
    CONSTRAINT fk_sqr_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

CREATE TABLE supplier_quotation_response_items (
    id                              UUID            NOT NULL,
    response_id                     UUID            NOT NULL,
    quotation_item_id               UUID            NOT NULL,
    unit_price                      NUMERIC(18, 4)  NOT NULL,
    quantity_available              NUMERIC(18, 4)  NULL,
    freight_amount                  NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    tax_amount                      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    discount_amount                 NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    lead_time_days                  INT             NULL,
    brand_offered                   VARCHAR(120)    NULL,
    line_total                      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    selected                        BOOLEAN         NOT NULL DEFAULT FALSE,
    quantity_selected               NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    notes                           VARCHAR(1000)   NULL,
    active                          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                      UUID            NULL,
    updated_by                      UUID            NULL,
    version                         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sqr_items PRIMARY KEY (id),
    CONSTRAINT uk_sqr_items_line UNIQUE (response_id, quotation_item_id),
    CONSTRAINT fk_sqr_items_response FOREIGN KEY (response_id) REFERENCES supplier_quotation_responses (id) ON DELETE CASCADE,
    CONSTRAINT fk_sqr_items_qi FOREIGN KEY (quotation_item_id) REFERENCES purchase_quotation_items (id),
    CONSTRAINT ck_sqr_items_price CHECK (unit_price >= 0 AND quantity_selected >= 0)
);

CREATE TABLE purchase_quotation_status_history (
    id                      UUID            NOT NULL,
    purchase_quotation_id   UUID            NOT NULL,
    from_status             VARCHAR(30)     NULL,
    to_status               VARCHAR(30)     NOT NULL,
    notes                   VARCHAR(1000)   NULL,
    changed_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by              UUID            NULL,
    CONSTRAINT pk_pq_status_history PRIMARY KEY (id),
    CONSTRAINT fk_pq_status_hist_q FOREIGN KEY (purchase_quotation_id) REFERENCES purchase_quotations (id) ON DELETE CASCADE,
    CONSTRAINT fk_pq_status_hist_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_pq_status_hist ON purchase_quotation_status_history (purchase_quotation_id, changed_at);

COMMENT ON TABLE purchase_quotations IS 'Cotação de compra multipornecedor (Prompt 60); tipo documental SUPPLIER_QUOTATION';
