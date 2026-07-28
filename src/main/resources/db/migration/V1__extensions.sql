-- V1: extensões necessárias do PostgreSQL
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

COMMENT ON EXTENSION "pgcrypto" IS 'Funções criptográficas e geração de UUID (gen_random_uuid)';
