-- V216: API pública (Prompt 81)
CREATE TABLE public_api_credentials (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    client_id               VARCHAR(80)     NOT NULL,
    client_secret_hash      VARCHAR(100)    NOT NULL,
    name                    VARCHAR(160)    NOT NULL,
    scopes                  VARCHAR(1000)   NOT NULL,
    rate_limit_per_minute   INT             NOT NULL DEFAULT 60,
    revoked_at              TIMESTAMPTZ     NULL,
    last_used_at            TIMESTAMPTZ     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_public_api_credentials PRIMARY KEY (id),
    CONSTRAINT uk_pac_client UNIQUE (client_id),
    CONSTRAINT fk_pac_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE public_api_access_logs (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    credential_id           UUID            NULL,
    client_id               VARCHAR(80)     NOT NULL,
    method                  VARCHAR(10)     NOT NULL,
    path                    VARCHAR(500)    NOT NULL,
    status_code             INT             NOT NULL,
    scopes_used             VARCHAR(500)    NULL,
    correlation_id          VARCHAR(80)     NULL,
    idempotency_key         VARCHAR(120)    NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_public_api_access_logs PRIMARY KEY (id),
    CONSTRAINT fk_paal_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_paal_cred FOREIGN KEY (credential_id) REFERENCES public_api_credentials (id)
);

CREATE INDEX idx_paal_org_created ON public_api_access_logs (organization_id, created_at DESC);
CREATE INDEX idx_pac_org ON public_api_credentials (organization_id);

COMMENT ON TABLE public_api_credentials IS 'Credenciais client_credentials da API pública (Prompt 81); secret só em hash';
COMMENT ON TABLE public_api_access_logs IS 'Logs de acesso sem secrets';
