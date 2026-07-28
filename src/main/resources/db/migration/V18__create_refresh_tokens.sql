-- V18: refresh tokens (armazenados apenas como hash)
CREATE TABLE refresh_tokens (
    id                      UUID            NOT NULL,
    user_id                 UUID            NOT NULL,
    token_hash              VARCHAR(64)     NOT NULL,
    expires_at              TIMESTAMPTZ     NOT NULL,
    revoked_at              TIMESTAMPTZ     NULL,
    replaced_by_token_id    UUID            NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    user_agent              VARCHAR(500)    NULL,
    ip_address              VARCHAR(45)     NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_revoked_at ON refresh_tokens (revoked_at);

COMMENT ON TABLE refresh_tokens IS 'Refresh tokens com hash SHA-256; suporte a revogação e rotação';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 do refresh token em hexadecimal';
