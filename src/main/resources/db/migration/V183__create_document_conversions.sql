-- V183: fundação de rastreabilidade documental (Prompt 56 — DOCUMENT_TRACEABILITY)
-- Conversões tipadas entre documentos; FKs específicas nos documentos permanecem para join rápido.

CREATE TABLE document_conversions (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NULL,
    from_type               VARCHAR(40)     NOT NULL,
    from_id                 UUID            NOT NULL,
    from_number             VARCHAR(40)     NULL,
    to_type                 VARCHAR(40)     NOT NULL,
    to_id                   UUID            NOT NULL,
    to_number               VARCHAR(40)     NULL,
    converted_at            TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    converted_by_user_id    UUID            NULL,
    notes                   VARCHAR(1000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_document_conversions PRIMARY KEY (id),
    CONSTRAINT fk_document_conversions_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_document_conversions_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_document_conversions_user FOREIGN KEY (converted_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_document_conversions_types CHECK (
        from_type IN (
            'PURCHASE_REQUEST', 'SUPPLIER_QUOTATION', 'PURCHASE_ORDER', 'PURCHASE_RECEIPT', 'SUPPLIER_RETURN',
            'QUOTE', 'SALES_ORDER', 'STOCK_RESERVATION', 'PICKING_ORDER', 'SHIPMENT', 'SALE', 'INVOICE_PROCESS'
        )
        AND to_type IN (
            'PURCHASE_REQUEST', 'SUPPLIER_QUOTATION', 'PURCHASE_ORDER', 'PURCHASE_RECEIPT', 'SUPPLIER_RETURN',
            'QUOTE', 'SALES_ORDER', 'STOCK_RESERVATION', 'PICKING_ORDER', 'SHIPMENT', 'SALE', 'INVOICE_PROCESS'
        )
    )
);

CREATE INDEX idx_document_conversions_from ON document_conversions (from_type, from_id);
CREATE INDEX idx_document_conversions_to ON document_conversions (to_type, to_id);
CREATE INDEX idx_document_conversions_org ON document_conversions (organization_id);
CREATE INDEX idx_document_conversions_store ON document_conversions (store_id);

CREATE TABLE document_conversion_items (
    id                      UUID            NOT NULL,
    conversion_id           UUID            NOT NULL,
    from_item_id            UUID            NULL,
    to_item_id              UUID            NULL,
    quantity_source         NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    quantity_converted      NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    quantity_remaining      NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    CONSTRAINT pk_document_conversion_items PRIMARY KEY (id),
    CONSTRAINT fk_document_conversion_items_conv FOREIGN KEY (conversion_id)
        REFERENCES document_conversions (id) ON DELETE CASCADE,
    CONSTRAINT ck_document_conversion_items_qty CHECK (
        quantity_source >= 0 AND quantity_converted >= 0 AND quantity_remaining >= 0
    )
);

CREATE INDEX idx_document_conversion_items_conv ON document_conversion_items (conversion_id);

COMMENT ON TABLE document_conversions IS 'Histórico imutável de conversões documento→documento (Prompt 56)';
COMMENT ON TABLE document_conversion_items IS 'Itens e saldos convertidos por conversão documental';

-- Liga Sale ao pedido de origem (rastreabilidade SO → Sale)
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS sales_order_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_sales_sales_order'
    ) THEN
        ALTER TABLE sales
            ADD CONSTRAINT fk_sales_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sales_sales_order ON sales (sales_order_id);

COMMENT ON COLUMN sales.sales_order_id IS 'Pedido de venda de origem quando a Sale veio de faturamento';
