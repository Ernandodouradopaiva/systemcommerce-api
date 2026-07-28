-- V273: Fila de emissão / dead-letter (Prompt 144)

CREATE TABLE fiscal_emission_queue_items (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    store_id                    UUID            NULL,
    establishment_id            UUID            NOT NULL,
    document_id                 UUID            NOT NULL,
    queue_name                  VARCHAR(40)     NOT NULL,
    status                      VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    priority                    INT             NOT NULL DEFAULT 100,
    attempts                    INT             NOT NULL DEFAULT 0,
    max_attempts                INT             NOT NULL DEFAULT 5,
    next_attempt_at             TIMESTAMPTZ     NULL,
    last_error                  TEXT            NULL,
    last_cstat                  VARCHAR(10)     NULL,
    communication_failure       BOOLEAN         NOT NULL DEFAULT FALSE,
    correlation_id              VARCHAR(80)     NULL,
    idempotency_key             VARCHAR(80)     NOT NULL,
    locked_by                   VARCHAR(80)     NULL,
    locked_at                   TIMESTAMPTZ     NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_emission_queue_items PRIMARY KEY (id),
    CONSTRAINT uk_feq_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_feq_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_feq_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_feq_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id),
    CONSTRAINT fk_feq_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT ck_feq_queue CHECK (queue_name IN ('EMISSION', 'RETRANSMIT', 'EVENT', 'CANCEL')),
    CONSTRAINT ck_feq_status CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED', 'DEAD_LETTER'))
);

CREATE INDEX idx_feq_status_next ON fiscal_emission_queue_items (status, next_attempt_at);
CREATE INDEX idx_feq_document ON fiscal_emission_queue_items (document_id);

CREATE TABLE fiscal_dead_letter_items (
    id                          UUID            NOT NULL,
    queue_item_id               UUID            NOT NULL,
    document_id                 UUID            NOT NULL,
    reason                      TEXT            NOT NULL,
    payload_json                TEXT            NULL,
    resolved                    BOOLEAN         NOT NULL DEFAULT FALSE,
    resolved_at                 TIMESTAMPTZ     NULL,
    resolved_by                 UUID            NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_dead_letter_items PRIMARY KEY (id),
    CONSTRAINT fk_fdl_queue FOREIGN KEY (queue_item_id) REFERENCES fiscal_emission_queue_items (id),
    CONSTRAINT fk_fdl_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id)
);
