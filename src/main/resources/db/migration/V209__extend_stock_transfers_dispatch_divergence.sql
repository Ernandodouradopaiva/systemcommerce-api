-- V209: Refino transferência — dispatch documentado + divergências tipadas (Prompt 75)
-- StockTransfer já cobre o fluxo principal (V161); adiciona entidades explícitas pedidas.

CREATE TABLE stock_transfer_dispatches (
    id                      UUID            NOT NULL,
    transfer_id             UUID            NOT NULL,
    dispatched_at           TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    dispatched_by           UUID            NULL,
    notes                   VARCHAR(1000)   NULL,
    idempotency_key         VARCHAR(80)     NULL,
    CONSTRAINT pk_stock_transfer_dispatches PRIMARY KEY (id),
    CONSTRAINT fk_std_transfer FOREIGN KEY (transfer_id) REFERENCES stock_transfers (id) ON DELETE CASCADE,
    CONSTRAINT fk_std_user FOREIGN KEY (dispatched_by) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_std_idempotency ON stock_transfer_dispatches (transfer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE stock_transfer_divergences (
    id                      UUID            NOT NULL,
    transfer_id             UUID            NOT NULL,
    item_id                 UUID            NULL,
    receipt_id              UUID            NULL,
    divergence_type         VARCHAR(40)     NOT NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    description             VARCHAR(1000)   NOT NULL,
    resolved                BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    CONSTRAINT pk_stock_transfer_divergences PRIMARY KEY (id),
    CONSTRAINT fk_stdv_transfer FOREIGN KEY (transfer_id) REFERENCES stock_transfers (id) ON DELETE CASCADE,
    CONSTRAINT fk_stdv_item FOREIGN KEY (item_id) REFERENCES stock_transfer_items (id),
    CONSTRAINT fk_stdv_receipt FOREIGN KEY (receipt_id) REFERENCES stock_transfer_receipts (id),
    CONSTRAINT ck_stdv_type CHECK (divergence_type IN (
        'SHORTAGE', 'EXCESS', 'DAMAGE', 'WRONG_PRODUCT', 'OTHER'
    )),
    CONSTRAINT ck_stdv_qty CHECK (quantity <> 0)
);

COMMENT ON TABLE stock_transfer_dispatches IS 'Registro explícito de expedição (Prompt 75)';
COMMENT ON TABLE stock_transfer_divergences IS 'Divergências obrigatórias no recebimento (Prompt 75)';
