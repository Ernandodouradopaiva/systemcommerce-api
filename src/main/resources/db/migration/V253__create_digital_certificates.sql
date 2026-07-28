-- V253: Certificados digitais (Prompt 123)
CREATE TABLE digital_certificates (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    type                    VARCHAR(10)     NOT NULL,
    holder_name             VARCHAR(200)    NULL,
    cnpj                    VARCHAR(14)     NULL,
    issuer_name             VARCHAR(300)    NULL,
    serial_number           VARCHAR(100)    NULL,
    valid_from              TIMESTAMPTZ     NULL,
    valid_until             TIMESTAMPTZ     NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    storage_ref             VARCHAR(100)    NOT NULL,
    thumbprint              VARCHAR(128)    NOT NULL,
    encrypted_keystore      TEXT            NULL,
    encrypted_password      TEXT            NULL,
    last_tested_at          TIMESTAMPTZ     NULL,
    last_test_result        VARCHAR(40)     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_digital_certificates PRIMARY KEY (id),
    CONSTRAINT fk_dc_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_dc_type CHECK (type IN ('A1', 'A3')),
    CONSTRAINT ck_dc_status CHECK (status IN ('PENDING', 'VALID', 'ACTIVE', 'EXPIRED', 'REVOKED', 'INACTIVE')),
    CONSTRAINT uk_dc_org_thumbprint UNIQUE (organization_id, thumbprint)
);

CREATE INDEX idx_dc_org_status ON digital_certificates (organization_id, status);
CREATE INDEX idx_dc_valid_until ON digital_certificates (valid_until);

CREATE TABLE certificate_assignments (
    id                      UUID            NOT NULL,
    certificate_id          UUID            NOT NULL,
    establishment_id        UUID            NOT NULL,
    environment             VARCHAR(20)     NOT NULL,
    primary_assignment      BOOLEAN         NOT NULL DEFAULT TRUE,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_certificate_assignments PRIMARY KEY (id),
    CONSTRAINT uk_ca_est_env_cert UNIQUE (establishment_id, environment, certificate_id),
    CONSTRAINT fk_ca_cert FOREIGN KEY (certificate_id) REFERENCES digital_certificates (id),
    CONSTRAINT fk_ca_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id),
    CONSTRAINT ck_ca_env CHECK (environment IN ('HOMOLOGATION', 'PRODUCTION')),
    CONSTRAINT ck_ca_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_ca_est_env ON certificate_assignments (establishment_id, environment);

CREATE TABLE certificate_validation_history (
    id                  UUID            NOT NULL,
    certificate_id      UUID            NOT NULL,
    validated_at        TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    validated_by        UUID            NULL,
    result              VARCHAR(40)     NOT NULL,
    message             VARCHAR(2000)   NULL,
    CONSTRAINT pk_cert_validation_history PRIMARY KEY (id),
    CONSTRAINT fk_cvh_cert FOREIGN KEY (certificate_id) REFERENCES digital_certificates (id) ON DELETE CASCADE
);

CREATE INDEX idx_cvh_cert ON certificate_validation_history (certificate_id, validated_at DESC);

CREATE TABLE certificate_usage_logs (
    id                  UUID            NOT NULL,
    certificate_id      UUID            NOT NULL,
    establishment_id    UUID            NULL,
    used_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    purpose             VARCHAR(80)     NOT NULL,
    correlation_id      VARCHAR(64)     NULL,
    performed_by        UUID            NULL,
    CONSTRAINT pk_cert_usage_logs PRIMARY KEY (id),
    CONSTRAINT fk_cul_cert FOREIGN KEY (certificate_id) REFERENCES digital_certificates (id) ON DELETE CASCADE,
    CONSTRAINT fk_cul_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id)
);

CREATE INDEX idx_cul_cert ON certificate_usage_logs (certificate_id, used_at DESC);

COMMENT ON TABLE digital_certificates IS 'Certificados A1/A3 — keystore/senha cifrados; nunca expostos na API de consulta (Prompt 123)';
COMMENT ON COLUMN digital_certificates.encrypted_keystore IS 'PKCS12 cifrado (AES-GCM); não retornar em DTO';
COMMENT ON COLUMN digital_certificates.encrypted_password IS 'Senha cifrada; nunca texto puro';
