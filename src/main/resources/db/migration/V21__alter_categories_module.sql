-- V21: categorias com situação e categoria pai
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS parent_id UUID NULL,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE categories
SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END;

ALTER TABLE categories
    DROP CONSTRAINT IF EXISTS fk_categories_parent;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id);

ALTER TABLE categories
    DROP CONSTRAINT IF EXISTS ck_categories_status;

ALTER TABLE categories
    ADD CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_status ON categories (status);

COMMENT ON COLUMN categories.parent_id IS 'Categoria pai opcional (hierarquia)';
COMMENT ON COLUMN categories.status IS 'ACTIVE | INACTIVE';
