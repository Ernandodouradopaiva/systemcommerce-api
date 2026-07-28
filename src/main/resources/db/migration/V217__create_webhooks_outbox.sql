-- V217: Webhooks + outbox transacional (Prompt 82)
CREATE TABLE webhook_subscriptions (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    name                    VARCHAR(160)    NOT NULL,
    target_url              VARCHAR(1000)   NOT NULL,
    event_types             VARCHAR(2000)   NOT NULL,
    secret_encrypted        TEXT            NOT NULL,
    payload_version         VARCHAR(20)     NOT NULL DEFAULT 'v1',
    status                  VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    consecutive_failures    INT             NOT NULL DEFAULT 0,
    max_failures            INT             NOT NULL DEFAULT 10,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_webhook_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_ws_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_ws_status CHECK (status IN ('ACTIVE', 'PAUSED', 'DISABLED'))
);

CREATE TABLE webhook_secrets (
    id                      UUID            NOT NULL,
    subscription_id         UUID            NOT NULL,
    secret_encrypted        TEXT            NOT NULL,
    valid_from              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_until             TIMESTAMPTZ     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_webhook_secrets PRIMARY KEY (id),
    CONSTRAINT fk_wsec_sub FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions (id) ON DELETE CASCADE
);

CREATE TABLE integration_outbox_events (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    event_type              VARCHAR(80)     NOT NULL,
    aggregate_type          VARCHAR(80)     NULL,
    aggregate_id            UUID            NULL,
    payload_json            TEXT            NOT NULL,
    payload_version         VARCHAR(20)     NOT NULL DEFAULT 'v1',
    idempotency_key         VARCHAR(160)    NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    available_at            TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    published_at            TIMESTAMPTZ     NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_integration_outbox PRIMARY KEY (id),
    CONSTRAINT uk_outbox_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT fk_outbox_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_pending ON integration_outbox_events (status, available_at)
    WHERE status = 'PENDING';

CREATE TABLE webhook_deliveries (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    subscription_id         UUID            NOT NULL,
    outbox_event_id         UUID            NULL,
    event_type              VARCHAR(80)     NOT NULL,
    payload_json            TEXT            NOT NULL,
    payload_version         VARCHAR(20)     NOT NULL DEFAULT 'v1',
    idempotency_key         VARCHAR(160)    NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    attempt_count           INT             NOT NULL DEFAULT 0,
    next_attempt_at         TIMESTAMPTZ     NULL,
    last_status_code        INT             NULL,
    last_error              VARCHAR(2000)   NULL,
    delivered_at            TIMESTAMPTZ     NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_webhook_deliveries PRIMARY KEY (id),
    CONSTRAINT uk_wd_idempotency UNIQUE (subscription_id, idempotency_key),
    CONSTRAINT fk_wd_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_wd_sub FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions (id),
    CONSTRAINT fk_wd_outbox FOREIGN KEY (outbox_event_id) REFERENCES integration_outbox_events (id),
    CONSTRAINT ck_wd_status CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED', 'DEAD_LETTER'
    ))
);

CREATE TABLE webhook_attempts (
    id                      UUID            NOT NULL,
    delivery_id             UUID            NOT NULL,
    attempt_number          INT             NOT NULL,
    request_headers_json    TEXT            NULL,
    response_status         INT             NULL,
    response_body           VARCHAR(4000)   NULL,
    error_message           VARCHAR(2000)   NULL,
    duration_ms             INT             NULL,
    attempted_at            TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_webhook_attempts PRIMARY KEY (id),
    CONSTRAINT fk_wa_delivery FOREIGN KEY (delivery_id) REFERENCES webhook_deliveries (id) ON DELETE CASCADE
);

CREATE INDEX idx_wd_pending ON webhook_deliveries (status, next_attempt_at)
    WHERE status IN ('PENDING', 'FAILED');

COMMENT ON TABLE integration_outbox_events IS 'Outbox transacional — não dispara HTTP na TX principal (Prompt 82)';
