-- V17: campos de autenticação e bloqueio em users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS login VARCHAR(100),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ NULL;

UPDATE users
SET login = LOWER(SPLIT_PART(email, '@', 1))
WHERE login IS NULL OR LENGTH(TRIM(login)) = 0;

-- Garante unicidade de login quando houver colisão no backfill
UPDATE users u
SET login = LOWER(SPLIT_PART(u.email, '@', 1)) || '_' || SUBSTRING(REPLACE(u.id::text, '-', '') FROM 1 FOR 8)
WHERE EXISTS (
    SELECT 1
    FROM users x
    WHERE LOWER(x.login) = LOWER(u.login)
      AND x.id <> u.id
);

ALTER TABLE users
    ALTER COLUMN login SET NOT NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS uk_users_login;

ALTER TABLE users
    ADD CONSTRAINT uk_users_login UNIQUE (login);

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS ck_users_status;

ALTER TABLE users
    ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS ck_users_failed_attempts;

ALTER TABLE users
    ADD CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0);

CREATE INDEX IF NOT EXISTS idx_users_login ON users (login);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);

COMMENT ON COLUMN users.login IS 'Identificador de login único (além do e-mail)';
COMMENT ON COLUMN users.status IS 'ACTIVE | INACTIVE | BLOCKED';
COMMENT ON COLUMN users.failed_login_attempts IS 'Contador de tentativas inválidas consecutivas';
COMMENT ON COLUMN users.locked_until IS 'Bloqueio temporário por tentativas excessivas';
COMMENT ON COLUMN users.last_login_at IS 'Data/hora do último login bem-sucedido (UTC)';
