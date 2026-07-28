-- V4: permissões granulares
CREATE TABLE permissions (
    id              UUID            NOT NULL,
    code            VARCHAR(80)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    module          VARCHAR(50)     NOT NULL,
    description     VARCHAR(255)    NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (code),
    CONSTRAINT ck_permissions_code_format CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_permissions_name_not_blank CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_permissions_module_not_blank CHECK (LENGTH(TRIM(module)) > 0)
);

CREATE INDEX idx_permissions_code ON permissions (code);
CREATE INDEX idx_permissions_module ON permissions (module);
CREATE INDEX idx_permissions_active ON permissions (active);

COMMENT ON TABLE permissions IS 'Permissões granulares avaliadas nos endpoints';
COMMENT ON COLUMN permissions.code IS 'Código estável (ex.: PRODUCT_CREATE, SALE_CANCEL)';
