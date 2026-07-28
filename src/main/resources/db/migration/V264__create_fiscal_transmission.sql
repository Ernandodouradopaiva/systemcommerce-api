-- V264: Endpoints SEFAZ e transmissão (Prompt 133)
CREATE TABLE fiscal_endpoint_registry (
    id                      UUID            NOT NULL,
    uf                      CHAR(2)         NOT NULL,
    model                   VARCHAR(10)     NOT NULL,
    environment             VARCHAR(20)     NOT NULL,
    service_name            VARCHAR(60)     NOT NULL,
    url                     VARCHAR(500)    NOT NULL,
    timeout_ms              INT             NOT NULL DEFAULT 30000,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_endpoint_registry PRIMARY KEY (id),
    CONSTRAINT uk_fer_uf_model_env_svc UNIQUE (uf, model, environment, service_name),
    CONSTRAINT ck_fer_env CHECK (environment IN ('HOMOLOGATION', 'PRODUCTION'))
);

CREATE TABLE fiscal_transmissions (
    id                      UUID            NOT NULL,
    document_id             UUID            NOT NULL,
    operation               VARCHAR(40)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    correlation_id          VARCHAR(100)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_transmissions PRIMARY KEY (id),
    CONSTRAINT fk_ft_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT ck_ft_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'SUCCESS', 'REJECTED', 'ERROR', 'TIMEOUT'))
);

CREATE INDEX idx_ft_doc ON fiscal_transmissions (document_id, created_at DESC);

CREATE TABLE fiscal_transmission_attempts (
    id                      UUID            NOT NULL,
    transmission_id         UUID            NOT NULL,
    attempt_number          INT             NOT NULL,
    request_digest          VARCHAR(64)     NULL,
    response_cstat          VARCHAR(10)     NULL,
    response_xmotivo        VARCHAR(500)    NULL,
    latency_ms              BIGINT          NULL,
    error_kind              VARCHAR(30)     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_transmission_attempts PRIMARY KEY (id),
    CONSTRAINT fk_fta_tx FOREIGN KEY (transmission_id) REFERENCES fiscal_transmissions (id) ON DELETE CASCADE,
    CONSTRAINT ck_fta_error CHECK (error_kind IS NULL OR error_kind IN ('NETWORK', 'FISCAL_REJECTION', 'TIMEOUT', 'UNKNOWN'))
);

-- Seeds CE homologação (URLs placeholder — registry aceita demais UFs)
INSERT INTO fiscal_endpoint_registry (
    id, uf, model, environment, service_name, url, timeout_ms, active, created_at, updated_at, version
) VALUES
    ('f2000000-0000-4000-8000-000000000001', 'CE', '55', 'HOMOLOGATION', 'NfeStatusServico',
     'https://nfeh.sefaz.ce.gov.br/nfe4/services/NFeStatusServico4', 30000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('f2000000-0000-4000-8000-000000000002', 'CE', '55', 'HOMOLOGATION', 'NfeAutorizacao',
     'https://nfeh.sefaz.ce.gov.br/nfe4/services/NFeAutorizacao4', 60000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('f2000000-0000-4000-8000-000000000003', 'CE', '65', 'HOMOLOGATION', 'NfeAutorizacao',
     'https://nfceh.sefaz.ce.gov.br/nfce4/services/NFeAutorizacao4', 60000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('f2000000-0000-4000-8000-000000000004', 'CE', '55', 'HOMOLOGATION', 'RecepcaoEvento',
     'https://nfeh.sefaz.ce.gov.br/nfe4/services/NFeRecepcaoEvento4', 30000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('f2000000-0000-4000-8000-000000000005', 'CE', '55', 'HOMOLOGATION', 'NfeInutilizacao',
     'https://nfeh.sefaz.ce.gov.br/nfe4/services/NFeInutilizacao4', 30000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);

COMMENT ON TABLE fiscal_endpoint_registry IS 'URLs SEFAZ por UF/modelo/ambiente (Prompt 133) — CE seed, demais UFs configuráveis';
