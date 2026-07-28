-- V280: Catálogo central Module / Resource / Action + vínculo em permissions (Prompt 152)

CREATE TABLE system_modules (
    id              UUID            NOT NULL,
    code            VARCHAR(40)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(255)    NULL,
    icon            VARCHAR(60)     NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    admin_visible   BOOLEAN         NOT NULL DEFAULT TRUE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_system_modules PRIMARY KEY (id),
    CONSTRAINT uk_system_modules_code UNIQUE (code)
);

CREATE TABLE system_resources (
    id              UUID            NOT NULL,
    module_id       UUID            NOT NULL,
    code            VARCHAR(60)     NOT NULL,
    name            VARCHAR(120)    NOT NULL,
    description     VARCHAR(255)    NULL,
    admin_route     VARCHAR(200)    NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_system_resources PRIMARY KEY (id),
    CONSTRAINT uk_system_resources_module_code UNIQUE (module_id, code),
    CONSTRAINT fk_sr_module FOREIGN KEY (module_id) REFERENCES system_modules (id)
);

CREATE TABLE system_actions (
    id              UUID            NOT NULL,
    code            VARCHAR(40)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(255)    NULL,
    generic         BOOLEAN         NOT NULL DEFAULT TRUE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_system_actions PRIMARY KEY (id),
    CONSTRAINT uk_system_actions_code UNIQUE (code)
);

ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS module_id UUID NULL,
    ADD COLUMN IF NOT EXISTS resource_id UUID NULL,
    ADD COLUMN IF NOT EXISTS action_id UUID NULL,
    ADD COLUMN IF NOT EXISTS system_permission BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS code_immutable BOOLEAN NOT NULL DEFAULT TRUE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perm_module') THEN
        ALTER TABLE permissions ADD CONSTRAINT fk_perm_module FOREIGN KEY (module_id) REFERENCES system_modules (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perm_resource') THEN
        ALTER TABLE permissions ADD CONSTRAINT fk_perm_resource FOREIGN KEY (resource_id) REFERENCES system_resources (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perm_action') THEN
        ALTER TABLE permissions ADD CONSTRAINT fk_perm_action FOREIGN KEY (action_id) REFERENCES system_actions (id);
    END IF;
END $$;

-- Modules
INSERT INTO system_modules (id, code, name, description, icon, sort_order, admin_visible, active, created_at, updated_at, version) VALUES
 ('c1000000-0000-4000-8000-000000000001', 'ADMIN', 'Administração', 'Administração geral', 'settings', 10, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000002', 'ACCESS', 'Acesso', 'Usuários, grupos e permissões', 'shield', 20, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000003', 'CADASTROS', 'Cadastros', 'Clientes, fornecedores e partes', 'users', 30, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000004', 'PRODUCTS', 'Produtos', 'Catálogo de produtos', 'package', 40, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000005', 'PURCHASES', 'Compras', 'Compras e recebimentos', 'shopping-cart', 50, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000006', 'INVENTORY', 'Estoque', 'Estoque e transferências', 'warehouse', 60, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000007', 'SALES', 'Vendas', 'Vendas administrativas', 'trending-up', 70, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000008', 'POS', 'PDV', 'Ponto de venda', 'monitor', 80, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-000000000009', 'FINANCE', 'Financeiro', 'Contas e tesouraria', 'dollar-sign', 90, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-00000000000a', 'FISCAL', 'Fiscal', 'Documentos fiscais eletrônicos', 'file-text', 100, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-00000000000b', 'REPORTS', 'Relatórios', 'Relatórios gerenciais', 'bar-chart', 110, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-00000000000c', 'AUDIT', 'Auditoria', 'Trilhas de auditoria', 'eye', 120, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c1000000-0000-4000-8000-00000000000d', 'INTEGRATIONS', 'Integrações', 'Integrações externas', 'plug', 130, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

-- Generic actions
INSERT INTO system_actions (id, code, name, description, generic, active, created_at, updated_at, version) VALUES
 ('c2000000-0000-4000-8000-000000000001', 'READ', 'Consultar', 'Consultar registros', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000002', 'CREATE', 'Incluir', 'Criar registros', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000003', 'UPDATE', 'Editar', 'Atualizar registros', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000004', 'DELETE', 'Excluir', 'Excluir registros', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000005', 'ACTIVATE', 'Ativar', 'Ativar registro', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000006', 'DISABLE', 'Inativar', 'Inativar registro', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000007', 'EXPORT', 'Exportar', 'Exportar dados', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000008', 'PRINT', 'Imprimir', 'Imprimir documentos', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000009', 'APPROVE', 'Aprovar', 'Aprovar operações', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-00000000000a', 'CANCEL', 'Cancelar', 'Cancelar operações', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-00000000000b', 'REVERSE', 'Estornar', 'Estornar operações', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-00000000000c', 'AUTHORIZE', 'Autorizar', 'Autorizar ações especiais', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-00000000000d', 'TRANSMIT', 'Transmitir', 'Transmitir à autoridade', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-00000000000e', 'REOPEN', 'Reabrir', 'Reabrir períodos/documentos', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-00000000000f', 'RECONCILE', 'Conciliar', 'Conciliar lançamentos', TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c2000000-0000-4000-8000-000000000010', 'MANAGE', 'Gerenciar', 'Gerenciar conjunto', FALSE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

-- Core ACL resources
INSERT INTO system_resources (id, module_id, code, name, description, admin_route, sort_order, active, created_at, updated_at, version) VALUES
 ('c3000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000002', 'USERS', 'Usuários', 'Cadastro de usuários', '/users', 10, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c3000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000002', 'ACCESS_GROUPS', 'Grupos de usuários', 'Grupos e permissões', '/access-groups', 20, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('c3000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000002', 'PERMISSIONS', 'Permissões', 'Catálogo de permissões', '/access-catalog', 30, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (module_id, code) DO NOTHING;

-- Backfill module_id from legacy string when possible
UPDATE permissions p
SET module_id = m.id
FROM system_modules m
WHERE p.module_id IS NULL
  AND UPPER(p.module) = m.code;

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'ACCESS')
WHERE p.module_id IS NULL AND p.code LIKE 'USER_%';

UPDATE permissions p
SET module_id = (SELECT id FROM system_modules WHERE code = 'ACCESS')
WHERE p.module_id IS NULL AND (p.code LIKE 'ROLE_%' OR p.code LIKE 'ACCESS_GROUP_%');
