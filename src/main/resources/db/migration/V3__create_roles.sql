-- V3: perfis (roles)
CREATE TABLE roles (
    id              UUID            NOT NULL,
    code            VARCHAR(50)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(255)    NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code),
    CONSTRAINT ck_roles_code_format CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_roles_name_not_blank CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_roles_code ON roles (code);
CREATE INDEX idx_roles_active ON roles (active);

COMMENT ON TABLE roles IS 'Perfis de acesso do sistema';
COMMENT ON COLUMN roles.code IS 'Código estável do perfil (ex.: ADMIN, SELLER)';
