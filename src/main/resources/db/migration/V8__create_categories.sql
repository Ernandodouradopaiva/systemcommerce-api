-- V8: categorias de produtos
CREATE TABLE categories (
    id              UUID            NOT NULL,
    name            VARCHAR(120)    NOT NULL,
    description     VARCHAR(500)    NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT ck_categories_name_not_blank CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_categories_name ON categories (name);
CREATE INDEX idx_categories_active ON categories (active);

COMMENT ON TABLE categories IS 'Categorias de classificação de produtos';
