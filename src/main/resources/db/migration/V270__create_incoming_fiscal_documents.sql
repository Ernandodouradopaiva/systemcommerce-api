-- V270: Entrada fiscal de compras / XML fornecedor (Prompt 142)
CREATE TABLE incoming_fiscal_documents (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    store_id                    UUID            NOT NULL,
    supplier_id                 UUID            NULL,
    access_key                  VARCHAR(44)     NOT NULL,
    model                       VARCHAR(10)     NOT NULL,
    series                      VARCHAR(10)     NULL,
    number                      BIGINT          NULL,
    issue_date                  DATE            NULL,
    xml_content                 TEXT            NOT NULL,
    xml_sha256                  VARCHAR(64)     NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'IMPORTED',
    authorization_protocol      VARCHAR(60)     NULL,
    signature_valid             BOOLEAN         NULL,
    authorized                  BOOLEAN         NULL,
    imported_at                 TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_incoming_fiscal_documents PRIMARY KEY (id),
    CONSTRAINT uk_ifd_access_key UNIQUE (access_key),
    CONSTRAINT fk_ifd_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ifd_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_ifd_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT ck_ifd_status CHECK (status IN ('IMPORTED', 'VALIDATED', 'LINKED', 'DIVERGENT', 'REJECTED'))
);

CREATE TABLE incoming_fiscal_document_items (
    id                      UUID            NOT NULL,
    incoming_id             UUID            NOT NULL,
    line                    INT             NOT NULL,
    product_id              UUID            NULL,
    external_code           VARCHAR(60)     NULL,
    description             VARCHAR(500)    NULL,
    ncm                     VARCHAR(10)     NULL,
    quantity                NUMERIC(19, 6)  NULL,
    unit_price              NUMERIC(19, 2)  NULL,
    total                   NUMERIC(19, 2)  NULL,
    matched                 BOOLEAN         NOT NULL DEFAULT FALSE,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_incoming_fiscal_document_items PRIMARY KEY (id),
    CONSTRAINT fk_ifdi_incoming FOREIGN KEY (incoming_id) REFERENCES incoming_fiscal_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_ifdi_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE incoming_fiscal_document_links (
    id                      UUID            NOT NULL,
    incoming_id             UUID            NOT NULL,
    link_type               VARCHAR(30)     NOT NULL,
    link_id                 UUID            NOT NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_incoming_fiscal_document_links PRIMARY KEY (id),
    CONSTRAINT uk_ifdl_type_link UNIQUE (incoming_id, link_type, link_id),
    CONSTRAINT fk_ifdl_incoming FOREIGN KEY (incoming_id) REFERENCES incoming_fiscal_documents (id) ON DELETE CASCADE,
    CONSTRAINT ck_ifdl_type CHECK (link_type IN ('PURCHASE_ORDER', 'PURCHASE_RECEIPT', 'SUPPLIER'))
);

CREATE TABLE incoming_fiscal_validations (
    id                      UUID            NOT NULL,
    incoming_id             UUID            NOT NULL,
    validated_at            TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    result                  VARCHAR(10)     NOT NULL,
    messages_json           TEXT            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_incoming_fiscal_validations PRIMARY KEY (id),
    CONSTRAINT fk_ifv_incoming FOREIGN KEY (incoming_id) REFERENCES incoming_fiscal_documents (id) ON DELETE CASCADE,
    CONSTRAINT ck_ifv_result CHECK (result IN ('OK', 'WARN', 'FAIL'))
);

CREATE TABLE incoming_fiscal_divergences (
    id                      UUID            NOT NULL,
    incoming_id             UUID            NOT NULL,
    item_id                 UUID            NULL,
    divergence_type         VARCHAR(20)     NOT NULL,
    expected_json           TEXT            NULL,
    actual_json             TEXT            NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_incoming_fiscal_divergences PRIMARY KEY (id),
    CONSTRAINT fk_ifdv_incoming FOREIGN KEY (incoming_id) REFERENCES incoming_fiscal_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_ifdv_item FOREIGN KEY (item_id) REFERENCES incoming_fiscal_document_items (id) ON DELETE SET NULL,
    CONSTRAINT ck_ifdv_type CHECK (divergence_type IN ('QTY', 'VALUE', 'PRODUCT', 'TAX')),
    CONSTRAINT ck_ifdv_status CHECK (status IN ('OPEN', 'ACCEPTED', 'RESOLVED'))
);

COMMENT ON TABLE incoming_fiscal_documents IS 'XML de entrada imutável (Prompt 142); estoque só via recebimento comercial';
COMMENT ON COLUMN incoming_fiscal_documents.xml_content IS 'XML original — nunca alterar após importação';
