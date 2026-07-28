-- V2: usuários do sistema
CREATE TABLE users (
    id              UUID            NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_not_blank CHECK (LENGTH(TRIM(email)) > 0),
    CONSTRAINT ck_users_name_not_blank CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_users_password_hash_not_blank CHECK (LENGTH(TRIM(password_hash)) > 0)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_active ON users (active);
CREATE INDEX idx_users_created_at ON users (created_at);

COMMENT ON TABLE users IS 'Usuários autenticáveis do SystemCommerce';
COMMENT ON COLUMN users.password_hash IS 'Hash BCrypt da senha; nunca armazenar texto puro';
COMMENT ON COLUMN users.version IS 'Controle de optimistic locking';
