-- V168: vínculo cliente-loja (multiloja) + origem do cadastro

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations (id),
    ADD COLUMN IF NOT EXISTS origin_store_id UUID REFERENCES stores (id);

UPDATE customers
SET organization_id = 'b1000000-0000-4000-8000-000000000001'
WHERE organization_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_customers_organization ON customers (organization_id);
CREATE INDEX IF NOT EXISTS idx_customers_origin_store ON customers (origin_store_id);

CREATE TABLE IF NOT EXISTS customer_store_relationships (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (id),
    store_id UUID NOT NULL REFERENCES stores (id),
    first_service_at TIMESTAMPTZ,
    last_purchase_at TIMESTAMPTZ,
    preferred_seller_profile_id UUID REFERENCES seller_profiles (id),
    local_notes VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_store_relationship UNIQUE (customer_id, store_id),
    CONSTRAINT ck_customer_store_relationship_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_customer_store_relationships_store ON customer_store_relationships (store_id, status);
CREATE INDEX IF NOT EXISTS idx_customer_store_relationships_customer ON customer_store_relationships (customer_id, status);

COMMENT ON TABLE customer_store_relationships IS 'Relacionamento local cliente-loja (sem duplicar cadastro de cliente)';
