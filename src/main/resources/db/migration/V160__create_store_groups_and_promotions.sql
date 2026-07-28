-- V160: grupos de lojas, canal de preço e promoções por loja

CREATE TABLE IF NOT EXISTS store_groups (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_store_groups_org_code UNIQUE (organization_id, code),
    CONSTRAINT ck_store_groups_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE IF NOT EXISTS store_group_members (
    id UUID PRIMARY KEY,
    store_group_id UUID NOT NULL REFERENCES store_groups (id),
    store_id UUID NOT NULL REFERENCES stores (id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_store_group_members UNIQUE (store_group_id, store_id)
);

CREATE INDEX IF NOT EXISTS idx_store_group_members_store ON store_group_members (store_id);

ALTER TABLE price_tables
    ADD COLUMN IF NOT EXISTS channel VARCHAR(20) NOT NULL DEFAULT 'ERP',
    ADD COLUMN IF NOT EXISTS scope_type VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    ADD COLUMN IF NOT EXISTS store_group_id UUID REFERENCES store_groups (id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_price_tables_channel') THEN
        ALTER TABLE price_tables
            ADD CONSTRAINT ck_price_tables_channel
                CHECK (channel IN ('ERP', 'POS', 'ONLINE', 'WHOLESALE', 'OTHER'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_price_tables_scope') THEN
        ALTER TABLE price_tables
            ADD CONSTRAINT ck_price_tables_scope
                CHECK (scope_type IN ('GLOBAL', 'STORE', 'STORE_GROUP'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    channel VARCHAR(20) NOT NULL DEFAULT 'POS',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    priority INTEGER NOT NULL DEFAULT 100,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_promotions_org_code UNIQUE (organization_id, code),
    CONSTRAINT ck_promotions_channel CHECK (channel IN ('ERP', 'POS', 'ONLINE', 'WHOLESALE', 'OTHER')),
    CONSTRAINT ck_promotions_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE IF NOT EXISTS promotion_stores (
    promotion_id UUID NOT NULL REFERENCES promotions (id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores (id),
    PRIMARY KEY (promotion_id, store_id)
);

CREATE TABLE IF NOT EXISTS promotion_products (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotions (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id),
    promotional_price NUMERIC(19, 2) NOT NULL,
    min_quantity NUMERIC(19, 3) NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_promotion_products UNIQUE (promotion_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_promotions_org ON promotions (organization_id);
CREATE INDEX IF NOT EXISTS idx_promotion_products_product ON promotion_products (product_id);

COMMENT ON COLUMN price_tables.channel IS 'Canal comercial: ERP, POS, ONLINE, WHOLESALE, OTHER';
COMMENT ON COLUMN price_tables.scope_type IS 'GLOBAL (sem vínculo), STORE (price_table_stores), STORE_GROUP';
COMMENT ON TABLE promotions IS 'Promoções por organização/canal/loja; resolução oficial na API';
