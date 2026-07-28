-- V260: Documento fiscal eletrônico base (Prompt 129)
CREATE TABLE fiscal_documents (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    establishment_id            UUID            NOT NULL,
    store_id                    UUID            NOT NULL,
    model                       VARCHAR(10)     NOT NULL,
    series                      VARCHAR(10)     NOT NULL,
    number                      BIGINT          NOT NULL,
    access_key                  VARCHAR(44)     NULL,
    environment                 VARCHAR(20)     NOT NULL,
    issue_date_time             TIMESTAMPTZ     NULL,
    entry_exit_date_time        TIMESTAMPTZ     NULL,
    nature_of_operation         VARCHAR(200)    NULL,
    purpose                     VARCHAR(40)     NULL,
    operation_id                UUID            NULL,
    direction                   VARCHAR(10)     NOT NULL DEFAULT 'OUT',
    recipient_party_type        VARCHAR(20)     NULL,
    recipient_party_id          UUID            NULL,
    recipient_snapshot_json     TEXT            NULL,
    emitter_snapshot_json       TEXT            NULL,
    carrier_id                  UUID            NULL,
    carrier_snapshot_json       TEXT            NULL,
    total_products              NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_discount              NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_freight               NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_tax                   NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_invoice               NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    tax_calculation_id          UUID            NULL,
    status                      VARCHAR(40)     NOT NULL DEFAULT 'DRAFT',
    sefaz_cstat                 VARCHAR(10)     NULL,
    sefaz_xmotivo               VARCHAR(500)    NULL,
    layout_version              VARCHAR(40)     NULL,
    application_version         VARCHAR(40)     NULL,
    idempotency_key             VARCHAR(100)    NOT NULL,
    origin_document_type        VARCHAR(40)     NULL,
    origin_document_id          UUID            NULL,
    contingency                 BOOLEAN         NOT NULL DEFAULT FALSE,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_documents PRIMARY KEY (id),
    CONSTRAINT uk_fiscal_documents_numbering UNIQUE (establishment_id, model, series, number, environment),
    CONSTRAINT uk_fiscal_documents_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT fk_fd_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_fd_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id),
    CONSTRAINT fk_fd_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_fd_op FOREIGN KEY (operation_id) REFERENCES fiscal_operations (id),
    CONSTRAINT fk_fd_tax_calc FOREIGN KEY (tax_calculation_id) REFERENCES tax_calculations (id),
    CONSTRAINT ck_fd_model CHECK (model IN ('55', '65')),
    CONSTRAINT ck_fd_env CHECK (environment IN ('HOMOLOGATION', 'PRODUCTION')),
    CONSTRAINT ck_fd_dir CHECK (direction IN ('IN', 'OUT'))
);

CREATE INDEX idx_fd_store_status ON fiscal_documents (store_id, status);
CREATE INDEX idx_fd_origin ON fiscal_documents (origin_document_type, origin_document_id);
CREATE INDEX idx_fd_access_key ON fiscal_documents (access_key);

CREATE TABLE fiscal_document_items (
    id                      UUID            NOT NULL,
    document_id             UUID            NOT NULL,
    line_number             INT             NOT NULL,
    product_id              UUID            NULL,
    product_snapshot_json   TEXT            NULL,
    ncm                     VARCHAR(10)     NULL,
    cest                    VARCHAR(10)     NULL,
    cfop                    VARCHAR(10)     NULL,
    quantity                NUMERIC(19, 6)  NOT NULL DEFAULT 0,
    unit_price              NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_amount            NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    tax_snapshot_json       TEXT            NULL,
    commercial_uom          VARCHAR(10)     NULL,
    taxable_uom             VARCHAR(10)     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_document_items PRIMARY KEY (id),
    CONSTRAINT fk_fdi_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_document_payments (
    id                          UUID            NOT NULL,
    document_id                 UUID            NOT NULL,
    payment_method_fiscal_code  VARCHAR(10)     NULL,
    amount                      NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    indicator                   VARCHAR(10)     NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_document_payments PRIMARY KEY (id),
    CONSTRAINT fk_fdp_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_document_references (
    id                  UUID            NOT NULL,
    document_id         UUID            NOT NULL,
    ref_type            VARCHAR(40)     NULL,
    ref_access_key      VARCHAR(44)     NULL,
    ref_document_id     UUID            NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_document_references PRIMARY KEY (id),
    CONSTRAINT fk_fdr_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_document_status_histories (
    id                  UUID            NOT NULL,
    document_id         UUID            NOT NULL,
    from_status         VARCHAR(40)     NULL,
    to_status           VARCHAR(40)     NOT NULL,
    at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    by_user             UUID            NULL,
    sefaz_cstat         VARCHAR(10)     NULL,
    sefaz_xmotivo       VARCHAR(500)    NULL,
    details             TEXT            NULL,
    CONSTRAINT pk_fiscal_document_status_histories PRIMARY KEY (id),
    CONSTRAINT fk_fdsh_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_document_protocols (
    id                  UUID            NOT NULL,
    document_id         UUID            NOT NULL,
    protocol_type       VARCHAR(40)     NULL,
    protocol_number     VARCHAR(60)     NULL,
    received_at         TIMESTAMPTZ     NULL,
    raw_ref             TEXT            NULL,
    CONSTRAINT pk_fiscal_document_protocols PRIMARY KEY (id),
    CONSTRAINT fk_fdp2_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_document_xmls (
    id                  UUID            NOT NULL,
    document_id         UUID            NOT NULL,
    kind                VARCHAR(40)     NOT NULL,
    content             TEXT            NULL,
    sha256              VARCHAR(64)     NULL,
    stored_at           TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_fiscal_document_xmls PRIMARY KEY (id),
    CONSTRAINT fk_fdx_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_document_events (
    id                  UUID            NOT NULL,
    document_id         UUID            NOT NULL,
    event_type          VARCHAR(40)     NOT NULL,
    sequence            INT             NOT NULL DEFAULT 1,
    status              VARCHAR(20)     NULL,
    protocol_number     VARCHAR(60)     NULL,
    payload_json        TEXT            NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_fiscal_document_events PRIMARY KEY (id),
    CONSTRAINT fk_fde_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id) ON DELETE CASCADE
);

COMMENT ON TABLE fiscal_documents IS 'DFe base comum 55/65 (Prompt 129) — autorizado imutável; XML com hash';
