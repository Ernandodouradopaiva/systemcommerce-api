-- V184: cadastro profissional de fornecedores (Prompt 57)
-- Estende `suppliers` (sem duplicar) e cria tabelas filhas: endereços, contatos,
-- dados bancários, condições comerciais (org/loja), produtos, histórico de status e documentos.

-- ========== 1. Backfill organization_id (fornecedor é sempre global na organização) ==========
UPDATE suppliers
SET organization_id = 'b1000000-0000-4000-8000-000000000001'
WHERE organization_id IS NULL;

ALTER TABLE suppliers
    ALTER COLUMN organization_id SET NOT NULL;

-- ========== 2. Novos campos cadastrais / bloqueio ==========
ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS municipal_registration VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS tax_contributor_indicator VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS category VARCHAR(60) NULL,
    ADD COLUMN IF NOT EXISTS blocked_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS blocked_reason VARCHAR(500) NULL;

ALTER TABLE suppliers
    ADD CONSTRAINT ck_suppliers_tax_contributor_indicator CHECK (
        tax_contributor_indicator IS NULL OR tax_contributor_indicator IN ('CONTRIBUTOR', 'EXEMPT', 'NON_CONTRIBUTOR')
    );

-- status agora aceita BLOCKED (fornecedor bloqueado não participa de novas compras)
ALTER TABLE suppliers
    DROP CONSTRAINT IF EXISTS ck_suppliers_status;

ALTER TABLE suppliers
    ADD CONSTRAINT ck_suppliers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));

-- CPF/CNPJ único por organização (era único globalmente)
ALTER TABLE suppliers
    DROP CONSTRAINT IF EXISTS uk_suppliers_document;

ALTER TABLE suppliers
    ADD CONSTRAINT uk_suppliers_org_document UNIQUE (organization_id, document);

COMMENT ON COLUMN suppliers.municipal_registration IS 'Inscrição municipal (Prompt 57)';
COMMENT ON COLUMN suppliers.tax_contributor_indicator IS 'Indicador de contribuinte de ICMS (Prompt 57)';
COMMENT ON COLUMN suppliers.category IS 'Categoria/segmento do fornecedor (Prompt 57)';
COMMENT ON COLUMN suppliers.blocked_at IS 'Data/hora do bloqueio comercial (Prompt 57)';
COMMENT ON COLUMN suppliers.blocked_reason IS 'Motivo do bloqueio comercial (Prompt 57)';

-- ========== 3. Endereços (COMMERCIAL | BILLING | DELIVERY | OTHER) ==========
CREATE TABLE supplier_addresses (
    id              UUID            NOT NULL,
    supplier_id     UUID            NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    zip_code        VARCHAR(10)     NULL,
    street          VARCHAR(200)    NULL,
    number          VARCHAR(20)     NULL,
    complement      VARCHAR(100)    NULL,
    district        VARCHAR(100)    NULL,
    city            VARCHAR(100)    NULL,
    state           VARCHAR(2)      NULL,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_addresses PRIMARY KEY (id),
    CONSTRAINT fk_supplier_addresses_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT ck_supplier_addresses_type CHECK (type IN ('COMMERCIAL', 'BILLING', 'DELIVERY', 'OTHER'))
);

CREATE INDEX idx_supplier_addresses_supplier ON supplier_addresses (supplier_id);

-- ========== 4. Contatos (GENERAL | FINANCIAL | COMMERCIAL | OTHER) ==========
CREATE TABLE supplier_contacts (
    id              UUID            NOT NULL,
    supplier_id     UUID            NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    phone           VARCHAR(30)     NULL,
    email           VARCHAR(255)    NULL,
    role            VARCHAR(100)    NULL,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_contacts PRIMARY KEY (id),
    CONSTRAINT fk_supplier_contacts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT ck_supplier_contacts_type CHECK (type IN ('GENERAL', 'FINANCIAL', 'COMMERCIAL', 'OTHER')),
    CONSTRAINT ck_supplier_contacts_name_not_blank CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_supplier_contacts_supplier ON supplier_contacts (supplier_id);

-- ========== 5. Dados bancários (acesso restrito por permissão dedicada) ==========
CREATE TABLE supplier_bank_accounts (
    id              UUID            NOT NULL,
    supplier_id     UUID            NOT NULL,
    bank_code       VARCHAR(10)     NULL,
    agency          VARCHAR(20)     NULL,
    account         VARCHAR(30)     NULL,
    account_type    VARCHAR(20)     NULL,
    pix_key         VARCHAR(140)    NULL,
    holder_name     VARCHAR(150)    NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_bank_accounts PRIMARY KEY (id),
    CONSTRAINT fk_supplier_bank_accounts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT ck_supplier_bank_accounts_type CHECK (account_type IS NULL OR account_type IN ('CHECKING', 'SAVINGS'))
);

CREATE INDEX idx_supplier_bank_accounts_supplier ON supplier_bank_accounts (supplier_id);

COMMENT ON TABLE supplier_bank_accounts IS 'Dados bancários — leitura/gestão exigem SUPPLIER_BANK_DATA_READ/MANAGE (Prompt 57)';

-- ========== 6. Condições comerciais padrão (nível organização) ==========
CREATE TABLE supplier_commercial_conditions (
    id                          UUID            NOT NULL,
    supplier_id                 UUID            NOT NULL,
    payment_term_days           INT             NULL,
    payment_condition           VARCHAR(200)    NULL,
    preferred_carrier_name      VARCHAR(150)    NULL,
    min_order_amount            NUMERIC(18, 2)  NULL,
    average_lead_time_days      INT             NULL,
    notes                       VARCHAR(2000)   NULL,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_commercial_conditions PRIMARY KEY (id),
    CONSTRAINT uk_supplier_commercial_conditions_supplier UNIQUE (supplier_id),
    CONSTRAINT fk_supplier_commercial_conditions_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT ck_supplier_commercial_conditions_term CHECK (payment_term_days IS NULL OR payment_term_days >= 0),
    CONSTRAINT ck_supplier_commercial_conditions_min_order CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CONSTRAINT ck_supplier_commercial_conditions_lead_time CHECK (average_lead_time_days IS NULL OR average_lead_time_days >= 0)
);

-- ========== 7. Condições/observações por loja (override opcional) ==========
CREATE TABLE supplier_store_conditions (
    id                          UUID            NOT NULL,
    supplier_id                 UUID            NOT NULL,
    store_id                    UUID            NOT NULL,
    notes                       VARCHAR(2000)   NULL,
    payment_term_days           INT             NULL,
    payment_condition           VARCHAR(200)    NULL,
    min_order_amount            NUMERIC(18, 2)  NULL,
    average_lead_time_days      INT             NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_store_conditions PRIMARY KEY (id),
    CONSTRAINT uk_supplier_store_conditions_supplier_store UNIQUE (supplier_id, store_id),
    CONSTRAINT fk_supplier_store_conditions_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_store_conditions_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_supplier_store_conditions_term CHECK (payment_term_days IS NULL OR payment_term_days >= 0),
    CONSTRAINT ck_supplier_store_conditions_min_order CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CONSTRAINT ck_supplier_store_conditions_lead_time CHECK (average_lead_time_days IS NULL OR average_lead_time_days >= 0)
);

CREATE INDEX idx_supplier_store_conditions_store ON supplier_store_conditions (store_id);

-- ========== 8. Produtos fornecidos (catálogo fornecedor x produto) ==========
CREATE TABLE supplier_products (
    id                  UUID            NOT NULL,
    supplier_id         UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    supplier_sku        VARCHAR(60)     NULL,
    last_purchase_price NUMERIC(18, 4)  NULL,
    lead_time_days      INT             NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_products PRIMARY KEY (id),
    CONSTRAINT uk_supplier_products_supplier_product UNIQUE (supplier_id, product_id),
    CONSTRAINT fk_supplier_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_products_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_supplier_products_price CHECK (last_purchase_price IS NULL OR last_purchase_price >= 0),
    CONSTRAINT ck_supplier_products_lead_time CHECK (lead_time_days IS NULL OR lead_time_days >= 0)
);

CREATE INDEX idx_supplier_products_supplier ON supplier_products (supplier_id);
CREATE INDEX idx_supplier_products_product ON supplier_products (product_id);

-- ========== 9. Histórico de status (nunca apagar) ==========
CREATE TABLE supplier_status_history (
    id              UUID            NOT NULL,
    supplier_id     UUID            NOT NULL,
    from_status     VARCHAR(20)     NULL,
    to_status       VARCHAR(20)     NOT NULL,
    notes           VARCHAR(1000)   NULL,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    changed_by      UUID            NULL,
    CONSTRAINT pk_supplier_status_history PRIMARY KEY (id),
    CONSTRAINT fk_supplier_status_history_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE
);

CREATE INDEX idx_supplier_status_history_supplier ON supplier_status_history (supplier_id);

-- ========== 10. Documentos (somente metadados — sem upload binário) ==========
CREATE TABLE supplier_documents (
    id              UUID            NOT NULL,
    supplier_id     UUID            NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    type            VARCHAR(60)     NULL,
    file_ref        VARCHAR(500)    NULL,
    uploaded_at     TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_documents PRIMARY KEY (id),
    CONSTRAINT fk_supplier_documents_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT ck_supplier_documents_name_not_blank CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_supplier_documents_supplier ON supplier_documents (supplier_id);

COMMENT ON TABLE supplier_documents IS 'Metadados de documentos do fornecedor — sem upload binário (Prompt 57)';

-- ========== 11. Backfill: endereço/contato flat → tabelas filhas ==========
INSERT INTO supplier_addresses (
    id, supplier_id, type, zip_code, street, number, complement, district, city, state,
    is_primary, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), s.id, 'COMMERCIAL', s.zip_code, s.street, s.number, s.complement,
    s.district, s.city, s.state, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM suppliers s
WHERE (s.zip_code IS NOT NULL OR s.street IS NOT NULL OR s.city IS NOT NULL OR s.district IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM supplier_addresses a WHERE a.supplier_id = s.id);

INSERT INTO supplier_contacts (
    id, supplier_id, type, name, phone, email, role, is_primary, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), s.id, 'GENERAL', s.contact_name, s.phone, s.email, NULL, TRUE, TRUE,
    NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM suppliers s
WHERE s.contact_name IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM supplier_contacts c WHERE c.supplier_id = s.id);

-- ========== 12. Backfill: histórico inicial de status ==========
INSERT INTO supplier_status_history (id, supplier_id, from_status, to_status, notes, changed_at, changed_by)
SELECT gen_random_uuid(), s.id, NULL, s.status, 'Migração — status inicial registrado (Prompt 57)',
       NOW() AT TIME ZONE 'UTC', NULL
FROM suppliers s
WHERE NOT EXISTS (SELECT 1 FROM supplier_status_history h WHERE h.supplier_id = s.id);

COMMENT ON TABLE suppliers IS 'Fornecedores PF/PJ — cadastro comercial (Prompt 56, estendido no Prompt 57)';
