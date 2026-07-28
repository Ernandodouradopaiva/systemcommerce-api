-- V276: Auditoria fiscal dedicada (Prompt 147)

CREATE TABLE fiscal_audit_events (
    id                          UUID            NOT NULL,
    organization_id             UUID            NULL,
    store_id                    UUID            NULL,
    establishment_id            UUID            NULL,
    user_id                     UUID            NULL,
    document_id                 UUID            NULL,
    action                      VARCHAR(60)     NOT NULL,
    entity_type                 VARCHAR(60)     NULL,
    entity_id                   UUID            NULL,
    occurred_at                 TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ip_address                  VARCHAR(64)     NULL,
    correlation_id              VARCHAR(80)     NULL,
    result                      VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS',
    result_code                 VARCHAR(20)     NULL,
    before_json                 TEXT            NULL,
    after_json                  TEXT            NULL,
    details                     TEXT            NULL,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_fiscal_audit_events PRIMARY KEY (id),
    CONSTRAINT ck_fae_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE INDEX idx_fae_org_occurred ON fiscal_audit_events (organization_id, occurred_at DESC);
CREATE INDEX idx_fae_document ON fiscal_audit_events (document_id);
CREATE INDEX idx_fae_action ON fiscal_audit_events (action);
CREATE INDEX idx_fae_user ON fiscal_audit_events (user_id);
