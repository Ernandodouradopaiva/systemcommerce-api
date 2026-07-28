-- V154: acesso de usuários por loja
CREATE TABLE user_store_access (
    id                      UUID            NOT NULL,
    user_id                 UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    start_date              DATE            NOT NULL,
    end_date                DATE            NULL,
    default_store           BOOLEAN         NOT NULL DEFAULT FALSE,
    access_type             VARCHAR(20)     NOT NULL DEFAULT 'PERMANENT',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    granted_by_id           UUID            NULL,
    reason                  VARCHAR(500)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_store_access PRIMARY KEY (id),
    CONSTRAINT fk_usa_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_usa_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_usa_granted_by FOREIGN KEY (granted_by_id) REFERENCES users (id),
    CONSTRAINT ck_usa_access_type CHECK (access_type IN ('PERMANENT', 'TEMPORARY', 'SUPPORT', 'ADMINISTRATIVE')),
    CONSTRAINT ck_usa_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_usa_period CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_usa_temp_period CHECK (
        access_type NOT IN ('TEMPORARY', 'SUPPORT') OR end_date IS NOT NULL
    )
);

CREATE INDEX idx_usa_user ON user_store_access (user_id);
CREATE INDEX idx_usa_store ON user_store_access (store_id);
CREATE INDEX idx_usa_status ON user_store_access (status);
CREATE INDEX idx_usa_default ON user_store_access (user_id, default_store)
    WHERE default_store = TRUE AND status = 'ACTIVE';

COMMENT ON TABLE user_store_access IS 'Acesso autorizado do usuário às lojas (histórico; não apagar)';

-- Seed: admin com acesso permanente + loja padrão LOJA-01
INSERT INTO user_store_access (
    id, user_id, store_id, start_date, end_date, default_store, access_type, status,
    granted_by_id, reason, active, created_at, updated_at, version
)
SELECT
    'b4000000-0000-4000-8000-000000000001',
    u.id,
    s.id,
    CURRENT_DATE,
    NULL,
    TRUE,
    'ADMINISTRATIVE',
    'ACTIVE',
    u.id,
    'Seed acesso admin LOJA-01',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM users u
CROSS JOIN stores s
WHERE u.login = 'admin'
  AND s.code = 'LOJA-01'
  AND NOT EXISTS (
      SELECT 1 FROM user_store_access x WHERE x.id = 'b4000000-0000-4000-8000-000000000001'
  );
