-- V283: Escopos avançados, lojas de grupo, hierarquia e equipes (Prompts 156–161)

-- Expandir CHECK de escopo em group_permission_assignments
ALTER TABLE group_permission_assignments DROP CONSTRAINT IF EXISTS ck_gpa_scope;
UPDATE group_permission_assignments SET scope = 'GLOBAL_SYSTEM' WHERE scope = 'GLOBAL';
ALTER TABLE group_permission_assignments
    ALTER COLUMN scope TYPE VARCHAR(30);
ALTER TABLE group_permission_assignments
    ADD CONSTRAINT ck_gpa_scope CHECK (scope IN (
        'GLOBAL_SYSTEM', 'ORGANIZATION', 'STORE_GROUP', 'STORE', 'OWN_RECORDS', 'TEAM_RECORDS'
    ));

ALTER TABLE roles DROP CONSTRAINT IF EXISTS ck_roles_default_scope;
UPDATE roles SET default_scope = 'GLOBAL_SYSTEM' WHERE default_scope = 'GLOBAL';
ALTER TABLE roles ALTER COLUMN default_scope TYPE VARCHAR(30);
ALTER TABLE roles
    ADD CONSTRAINT ck_roles_default_scope CHECK (default_scope IN (
        'GLOBAL_SYSTEM', 'ORGANIZATION', 'STORE_GROUP', 'STORE', 'OWN_RECORDS', 'TEAM_RECORDS'
    ));

CREATE TABLE IF NOT EXISTS permission_scopes (
    id              UUID            NOT NULL,
    code            VARCHAR(30)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(255)    NULL,
    breadth_rank    INT             NOT NULL DEFAULT 0,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID            NULL,
    updated_by      UUID            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_permission_scopes PRIMARY KEY (id),
    CONSTRAINT uk_permission_scopes_code UNIQUE (code)
);

INSERT INTO permission_scopes (id, code, name, description, breadth_rank, active, created_at, updated_at, version) VALUES
 ('d1000000-0000-4000-8000-000000000001', 'GLOBAL_SYSTEM', 'Sistema global', 'Acesso global do sistema', 100, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('d1000000-0000-4000-8000-000000000002', 'ORGANIZATION', 'Organização', 'Toda a organização', 80, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('d1000000-0000-4000-8000-000000000003', 'STORE_GROUP', 'Grupo de lojas', 'Conjunto de lojas', 60, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('d1000000-0000-4000-8000-000000000004', 'STORE', 'Loja', 'Loja específica', 40, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('d1000000-0000-4000-8000-000000000005', 'TEAM_RECORDS', 'Registros da equipe', 'Registros da equipe hierárquica', 20, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
 ('d1000000-0000-4000-8000-000000000006', 'OWN_RECORDS', 'Próprios registros', 'Somente registros sob responsabilidade do usuário', 10, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS group_store_assignments (
    id                  UUID            NOT NULL,
    group_id            UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    organization_id     UUID            NULL,
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_to            TIMESTAMPTZ     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    reason              VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_group_store_assignments PRIMARY KEY (id),
    CONSTRAINT uk_gsa_group_store UNIQUE (group_id, store_id),
    CONSTRAINT fk_gsa_group FOREIGN KEY (group_id) REFERENCES roles (id),
    CONSTRAINT fk_gsa_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_gsa_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_gsa_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS organizational_positions (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NOT NULL,
    description         VARCHAR(255)    NULL,
    level_rank          INT             NOT NULL DEFAULT 100,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_organizational_positions PRIMARY KEY (id),
    CONSTRAINT uk_org_pos_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_org_pos_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE IF NOT EXISTS organizational_hierarchies (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    parent_position_id  UUID            NULL,
    child_position_id   UUID            NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_organizational_hierarchies PRIMARY KEY (id),
    CONSTRAINT uk_org_hier_parent_child UNIQUE (parent_position_id, child_position_id),
    CONSTRAINT fk_org_hier_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_org_hier_parent FOREIGN KEY (parent_position_id) REFERENCES organizational_positions (id),
    CONSTRAINT fk_org_hier_child FOREIGN KEY (child_position_id) REFERENCES organizational_positions (id),
    CONSTRAINT ck_org_hier_no_self CHECK (parent_position_id IS NULL OR parent_position_id <> child_position_id)
);

CREATE TABLE IF NOT EXISTS teams (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NOT NULL,
    description         VARCHAR(255)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_teams PRIMARY KEY (id),
    CONSTRAINT uk_teams_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_teams_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_teams_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE IF NOT EXISTS team_members (
    id                  UUID            NOT NULL,
    team_id             UUID            NOT NULL,
    user_id             UUID            NOT NULL,
    position_id         UUID            NULL,
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_to            TIMESTAMPTZ     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_team_members PRIMARY KEY (id),
    CONSTRAINT uk_team_members_team_user UNIQUE (team_id, user_id),
    CONSTRAINT fk_tm_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tm_position FOREIGN KEY (position_id) REFERENCES organizational_positions (id),
    CONSTRAINT ck_tm_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS team_manager_assignments (
    id                  UUID            NOT NULL,
    team_id             UUID            NOT NULL,
    manager_user_id     UUID            NOT NULL,
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_to            TIMESTAMPTZ     NULL,
    primary_manager     BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_team_manager_assignments PRIMARY KEY (id),
    CONSTRAINT uk_tma_team_manager UNIQUE (team_id, manager_user_id),
    CONSTRAINT fk_tma_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_tma_manager FOREIGN KEY (manager_user_id) REFERENCES users (id),
    CONSTRAINT ck_tma_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS user_hierarchy_assignments (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    user_id             UUID            NOT NULL,
    manager_user_id     UUID            NULL,
    position_id         UUID            NULL,
    store_id            UUID            NULL,
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_to            TIMESTAMPTZ     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_hierarchy_assignments PRIMARY KEY (id),
    CONSTRAINT uk_uha_user_manager_store UNIQUE NULLS NOT DISTINCT (user_id, manager_user_id, store_id),
    CONSTRAINT fk_uha_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_uha_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_uha_manager FOREIGN KEY (manager_user_id) REFERENCES users (id),
    CONSTRAINT fk_uha_position FOREIGN KEY (position_id) REFERENCES organizational_positions (id),
    CONSTRAINT fk_uha_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_uha_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
    CONSTRAINT ck_uha_no_self CHECK (manager_user_id IS NULL OR manager_user_id <> user_id)
);

CREATE INDEX IF NOT EXISTS idx_gsa_group ON group_store_assignments (group_id);
CREATE INDEX IF NOT EXISTS idx_tm_user ON team_members (user_id);
CREATE INDEX IF NOT EXISTS idx_tma_manager ON team_manager_assignments (manager_user_id);
CREATE INDEX IF NOT EXISTS idx_uha_manager ON user_hierarchy_assignments (manager_user_id);
CREATE INDEX IF NOT EXISTS idx_uha_user ON user_hierarchy_assignments (user_id);

-- Seeds de cargos padrão
INSERT INTO organizational_positions (id, organization_id, code, name, description, level_rank, active, created_at, updated_at, version)
SELECT 'd2000000-0000-4000-8000-000000000001', o.id, 'DIRECTOR', 'Diretor', 'Direção', 10, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o WHERE o.code = 'ORG-DEFAULT'
ON CONFLICT DO NOTHING;
INSERT INTO organizational_positions (id, organization_id, code, name, description, level_rank, active, created_at, updated_at, version)
SELECT 'd2000000-0000-4000-8000-000000000002', o.id, 'REGIONAL_MANAGER', 'Gerente regional', 'Gerência regional', 20, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o WHERE o.code = 'ORG-DEFAULT'
ON CONFLICT DO NOTHING;
INSERT INTO organizational_positions (id, organization_id, code, name, description, level_rank, active, created_at, updated_at, version)
SELECT 'd2000000-0000-4000-8000-000000000003', o.id, 'STORE_MANAGER', 'Gerente de loja', 'Gerência de loja', 30, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o WHERE o.code = 'ORG-DEFAULT'
ON CONFLICT DO NOTHING;
INSERT INTO organizational_positions (id, organization_id, code, name, description, level_rank, active, created_at, updated_at, version)
SELECT 'd2000000-0000-4000-8000-000000000004', o.id, 'SUPERVISOR', 'Supervisor', 'Supervisão', 40, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o WHERE o.code = 'ORG-DEFAULT'
ON CONFLICT DO NOTHING;
INSERT INTO organizational_positions (id, organization_id, code, name, description, level_rank, active, created_at, updated_at, version)
SELECT 'd2000000-0000-4000-8000-000000000005', o.id, 'SELLER', 'Vendedor', 'Vendas operacionais', 50, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o WHERE o.code = 'ORG-DEFAULT'
ON CONFLICT DO NOTHING;

INSERT INTO organizational_hierarchies (id, organization_id, parent_position_id, child_position_id, active, created_at, updated_at, version)
SELECT gen_random_uuid(), o.id, p1.id, p2.id, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o
JOIN organizational_positions p1 ON p1.organization_id = o.id AND p1.code = 'DIRECTOR'
JOIN organizational_positions p2 ON p2.organization_id = o.id AND p2.code = 'REGIONAL_MANAGER'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM organizational_hierarchies h WHERE h.parent_position_id = p1.id AND h.child_position_id = p2.id);

INSERT INTO organizational_hierarchies (id, organization_id, parent_position_id, child_position_id, active, created_at, updated_at, version)
SELECT gen_random_uuid(), o.id, p1.id, p2.id, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o
JOIN organizational_positions p1 ON p1.organization_id = o.id AND p1.code = 'REGIONAL_MANAGER'
JOIN organizational_positions p2 ON p2.organization_id = o.id AND p2.code = 'STORE_MANAGER'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM organizational_hierarchies h WHERE h.parent_position_id = p1.id AND h.child_position_id = p2.id);

INSERT INTO organizational_hierarchies (id, organization_id, parent_position_id, child_position_id, active, created_at, updated_at, version)
SELECT gen_random_uuid(), o.id, p1.id, p2.id, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o
JOIN organizational_positions p1 ON p1.organization_id = o.id AND p1.code = 'STORE_MANAGER'
JOIN organizational_positions p2 ON p2.organization_id = o.id AND p2.code = 'SUPERVISOR'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM organizational_hierarchies h WHERE h.parent_position_id = p1.id AND h.child_position_id = p2.id);

INSERT INTO organizational_hierarchies (id, organization_id, parent_position_id, child_position_id, active, created_at, updated_at, version)
SELECT gen_random_uuid(), o.id, p1.id, p2.id, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o
JOIN organizational_positions p1 ON p1.organization_id = o.id AND p1.code = 'SUPERVISOR'
JOIN organizational_positions p2 ON p2.organization_id = o.id AND p2.code = 'SELLER'
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM organizational_hierarchies h WHERE h.parent_position_id = p1.id AND h.child_position_id = p2.id);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version, system_permission, code_immutable)
VALUES
    ('a1000000-0000-4000-8000-000000000389', 'EFFECTIVE_PERMISSION_READ', 'Consultar permissões efetivas', 'ACCESS',
     'Consultar e explicar permissões efetivas de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000390', 'HIERARCHY_READ', 'Consultar hierarquia', 'ACCESS',
     'Consultar cargos, equipes e hierarquia', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000391', 'HIERARCHY_MANAGE', 'Gerenciar hierarquia', 'ACCESS',
     'Gerenciar cargos, equipes e vínculos hierárquicos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('EFFECTIVE_PERMISSION_READ', 'HIERARCHY_READ', 'HIERARCHY_MANAGE')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO group_permission_assignments (
    id, group_id, permission_id, grant_type, scope, valid_from, status, active, created_at, updated_at, version)
SELECT gen_random_uuid(), r.id, p.id, 'ALLOW', 'ORGANIZATION', NOW() AT TIME ZONE 'UTC', 'ACTIVE', TRUE,
       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('EFFECTIVE_PERMISSION_READ', 'HIERARCHY_READ', 'HIERARCHY_MANAGE')
ON CONFLICT (group_id, permission_id) DO NOTHING;
