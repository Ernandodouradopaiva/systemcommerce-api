-- V215: Hub genérico de marketplaces (Prompt 80)
CREATE TABLE sales_channels (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    code                    VARCHAR(40)     NOT NULL,
    name                    VARCHAR(120)    NOT NULL,
    channel_type            VARCHAR(30)     NOT NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sales_channels PRIMARY KEY (id),
    CONSTRAINT uk_sales_channels_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_sc_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_sc_type CHECK (channel_type IN (
        'MERCADO_LIVRE', 'SHOPEE', 'WOOCOMMERCE', 'GENERIC', 'OTHER'
    ))
);

CREATE TABLE marketplace_accounts (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    sales_channel_id        UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    external_account_id     VARCHAR(120)    NULL,
    display_name            VARCHAR(160)    NOT NULL,
    credentials_encrypted   TEXT            NULL,
    settings_json           TEXT            NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    last_sync_at            TIMESTAMPTZ     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_marketplace_accounts PRIMARY KEY (id),
    CONSTRAINT fk_ma_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ma_channel FOREIGN KEY (sales_channel_id) REFERENCES sales_channels (id),
    CONSTRAINT fk_ma_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_ma_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_ma_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ERROR', 'DISCONNECTED'))
);

CREATE UNIQUE INDEX uk_ma_external ON marketplace_accounts (organization_id, sales_channel_id, external_account_id)
    WHERE external_account_id IS NOT NULL;

CREATE TABLE channel_products (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    marketplace_account_id  UUID            NOT NULL,
    product_id              UUID            NOT NULL,
    external_product_id     VARCHAR(120)    NOT NULL,
    external_sku            VARCHAR(120)    NULL,
    sync_status             VARCHAR(30)     NOT NULL DEFAULT 'LINKED',
    last_synced_at          TIMESTAMPTZ     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_channel_products PRIMARY KEY (id),
    CONSTRAINT uk_cp_external UNIQUE (marketplace_account_id, external_product_id),
    CONSTRAINT uk_cp_product UNIQUE (marketplace_account_id, product_id),
    CONSTRAINT fk_cp_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_cp_account FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id),
    CONSTRAINT fk_cp_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_cp_sync CHECK (sync_status IN ('LINKED', 'PENDING', 'ERROR', 'UNLINKED'))
);

CREATE TABLE channel_listings (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    marketplace_account_id  UUID            NOT NULL,
    channel_product_id      UUID            NOT NULL,
    external_listing_id     VARCHAR(120)    NOT NULL,
    title                   VARCHAR(500)    NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    published_price         NUMERIC(18, 4)  NULL,
    published_quantity      NUMERIC(18, 4)  NULL,
    last_synced_at          TIMESTAMPTZ     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_channel_listings PRIMARY KEY (id),
    CONSTRAINT uk_cl_external UNIQUE (marketplace_account_id, external_listing_id),
    CONSTRAINT fk_cl_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_cl_account FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id),
    CONSTRAINT fk_cl_cp FOREIGN KEY (channel_product_id) REFERENCES channel_products (id),
    CONSTRAINT ck_cl_status CHECK (status IN ('ACTIVE', 'PAUSED', 'CLOSED', 'ERROR'))
);

CREATE TABLE channel_orders (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    marketplace_account_id  UUID            NOT NULL,
    sales_order_id          UUID            NULL,
    external_order_id       VARCHAR(120)    NOT NULL,
    external_status         VARCHAR(80)     NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'RECEIVED',
    buyer_external_id       VARCHAR(120)    NULL,
    buyer_name              VARCHAR(200)    NULL,
    currency                VARCHAR(10)     NOT NULL DEFAULT 'BRL',
    total_amount            NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    raw_payload_json        TEXT            NULL,
    idempotency_key         VARCHAR(120)    NULL,
    received_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    converted_at            TIMESTAMPTZ     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_channel_orders PRIMARY KEY (id),
    CONSTRAINT uk_co_external UNIQUE (marketplace_account_id, external_order_id),
    CONSTRAINT fk_co_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_co_account FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id),
    CONSTRAINT fk_co_so FOREIGN KEY (sales_order_id) REFERENCES sales_orders (id),
    CONSTRAINT ck_co_status CHECK (status IN (
        'RECEIVED', 'MAPPED', 'CONVERTED', 'ERROR', 'IGNORED', 'CANCELLED'
    ))
);

CREATE UNIQUE INDEX uk_co_idempotency ON channel_orders (marketplace_account_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE channel_order_items (
    id                      UUID            NOT NULL,
    channel_order_id        UUID            NOT NULL,
    channel_product_id      UUID            NULL,
    product_id              UUID            NULL,
    external_item_id        VARCHAR(120)    NULL,
    external_sku            VARCHAR(120)    NULL,
    title                   VARCHAR(500)    NULL,
    quantity                NUMERIC(18, 4)  NOT NULL,
    unit_price              NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    line_total              NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    CONSTRAINT pk_channel_order_items PRIMARY KEY (id),
    CONSTRAINT fk_coi_order FOREIGN KEY (channel_order_id) REFERENCES channel_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_coi_cp FOREIGN KEY (channel_product_id) REFERENCES channel_products (id),
    CONSTRAINT fk_coi_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_coi_qty CHECK (quantity > 0)
);

CREATE TABLE channel_events (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    marketplace_account_id  UUID            NULL,
    event_type              VARCHAR(80)     NOT NULL,
    external_event_id       VARCHAR(160)    NULL,
    payload_json            TEXT            NULL,
    processed               BOOLEAN         NOT NULL DEFAULT FALSE,
    processed_at            TIMESTAMPTZ     NULL,
    error_message           VARCHAR(2000)   NULL,
    received_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_channel_events PRIMARY KEY (id),
    CONSTRAINT fk_ce_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ce_account FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id)
);

CREATE UNIQUE INDEX uk_ce_external ON channel_events (marketplace_account_id, external_event_id)
    WHERE external_event_id IS NOT NULL AND marketplace_account_id IS NOT NULL;

CREATE TABLE integration_jobs (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    marketplace_account_id  UUID            NULL,
    job_type                VARCHAR(60)     NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    attempt_count           INT             NOT NULL DEFAULT 0,
    max_attempts            INT             NOT NULL DEFAULT 5,
    next_attempt_at         TIMESTAMPTZ     NULL,
    payload_json            TEXT            NULL,
    last_error              VARCHAR(2000)   NULL,
    started_at              TIMESTAMPTZ     NULL,
    finished_at             TIMESTAMPTZ     NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_integration_jobs PRIMARY KEY (id),
    CONSTRAINT fk_ij_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ij_account FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id),
    CONSTRAINT ck_ij_status CHECK (status IN (
        'PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'FAILED_DEAD_LETTER', 'CANCELLED'
    ))
);

CREATE TABLE integration_errors (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    marketplace_account_id  UUID            NULL,
    integration_job_id      UUID            NULL,
    error_code              VARCHAR(80)     NULL,
    message                 VARCHAR(2000)   NOT NULL,
    detail_json             TEXT            NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_integration_errors PRIMARY KEY (id),
    CONSTRAINT fk_ie_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ie_account FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id),
    CONSTRAINT fk_ie_job FOREIGN KEY (integration_job_id) REFERENCES integration_jobs (id)
);

CREATE TABLE synchronization_checkpoints (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    marketplace_account_id  UUID            NOT NULL,
    sync_type               VARCHAR(60)     NOT NULL,
    cursor_value            VARCHAR(500)    NULL,
    last_success_at         TIMESTAMPTZ     NULL,
    metadata_json           TEXT            NULL,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_sync_checkpoints PRIMARY KEY (id),
    CONSTRAINT uk_sync_cp UNIQUE (marketplace_account_id, sync_type),
    CONSTRAINT fk_scp_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_scp_account FOREIGN KEY (marketplace_account_id) REFERENCES marketplace_accounts (id)
);

CREATE INDEX idx_ij_pending ON integration_jobs (status, next_attempt_at) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_co_account_status ON channel_orders (marketplace_account_id, status);

COMMENT ON TABLE sales_channels IS 'Canal de venda genérico (Prompt 80) — sem acoplamento a marketplace';
COMMENT ON TABLE marketplace_accounts IS 'Conta/conexão por canal; credenciais AES-GCM';
