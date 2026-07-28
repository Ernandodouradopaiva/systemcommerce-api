-- V197: Orçamento profissional — estende quotes (Prompt 64 — SalesQuotation = Quote)
ALTER TABLE quotes
    ADD COLUMN IF NOT EXISTS channel VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS price_table_id UUID NULL,
    ADD COLUMN IF NOT EXISTS payment_condition VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS carrier_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS expected_delivery_date DATE NULL,
    ADD COLUMN IF NOT EXISTS surcharge_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS revision_number INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS seller_profile_id UUID NULL,
    ADD COLUMN IF NOT EXISTS validity_days INT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quotes_price_table') THEN
        ALTER TABLE quotes ADD CONSTRAINT fk_quotes_price_table
            FOREIGN KEY (price_table_id) REFERENCES price_tables (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quotes_seller_profile') THEN
        ALTER TABLE quotes ADD CONSTRAINT fk_quotes_seller_profile
            FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles (id);
    END IF;
END $$;

ALTER TABLE quotes DROP CONSTRAINT IF EXISTS ck_quotes_status;
ALTER TABLE quotes ADD CONSTRAINT ck_quotes_status CHECK (status IN (
    'DRAFT', 'UNDER_REVIEW', 'UNDER_ANALYSIS', 'SENT', 'VIEWED', 'NEGOTIATING',
    'APPROVED', 'REJECTED', 'EXPIRED', 'CANCELLED', 'PARTIALLY_CONVERTED', 'CONVERTED'
));

-- Alias UNDER_REVIEW <-> UNDER_ANALYSIS
UPDATE quotes SET status = 'UNDER_ANALYSIS' WHERE status = 'UNDER_REVIEW';

ALTER TABLE quote_items
    ADD COLUMN IF NOT EXISTS quantity_converted NUMERIC(18, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS price_origin VARCHAR(60) NULL;

CREATE TABLE quote_revisions (
    id                  UUID            NOT NULL,
    quote_id            UUID            NOT NULL,
    revision_number     INT             NOT NULL,
    snapshot_json       TEXT            NOT NULL,
    change_notes        VARCHAR(1000)   NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    CONSTRAINT pk_quote_revisions PRIMARY KEY (id),
    CONSTRAINT uk_quote_revisions UNIQUE (quote_id, revision_number),
    CONSTRAINT fk_quote_revisions_quote FOREIGN KEY (quote_id) REFERENCES quotes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quote_revisions_user FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE quote_acceptances (
    id                  UUID            NOT NULL,
    quote_id            UUID            NOT NULL,
    accepted_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    accepted_by_name    VARCHAR(200)    NULL,
    accepted_by_email   VARCHAR(255)    NULL,
    acceptance_token    VARCHAR(80)     NULL,
    channel             VARCHAR(40)     NULL,
    notes               VARCHAR(1000)   NULL,
    CONSTRAINT pk_quote_acceptances PRIMARY KEY (id),
    CONSTRAINT fk_quote_acceptances_quote FOREIGN KEY (quote_id) REFERENCES quotes (id) ON DELETE CASCADE
);

CREATE INDEX idx_quote_revisions_quote ON quote_revisions (quote_id);
CREATE INDEX idx_quote_acceptances_quote ON quote_acceptances (quote_id);

COMMENT ON TABLE quotes IS 'SalesQuotation / Orçamento de venda (Prompt 64) — não baixa estoque';
