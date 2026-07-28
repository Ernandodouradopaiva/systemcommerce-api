-- V281: AccessGroup enrichment + assignments + history + access_version (Prompts 151–155)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS access_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS organization_id UUID NULL,
    ADD COLUMN IF NOT EXISTS group_type VARCHAR(20) NOT NULL DEFAULT 'OPERATIONAL',
    ADD COLUMN IF NOT EXISTS system_group BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS default_group BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS default_scope VARCHAR(20) NOT NULL DEFAULT 'ORGANIZATION',
    ADD COLUMN IF NOT EXISTS allows_administration BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS visual_priority INT NOT NULL DEFAULT 100;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_roles_organization') THEN
        ALTER TABLE roles ADD CONSTRAINT fk_roles_organization FOREIGN KEY (organization_id) REFERENCES organizations (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_roles_group_type') THEN
        ALTER TABLE roles ADD CONSTRAINT ck_roles_group_type
            CHECK (group_type IN ('SYSTEM', 'ADMINISTRATIVE', 'MANAGERIAL', 'OPERATIONAL', 'CUSTOM'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_roles_default_scope') THEN
        ALTER TABLE roles ADD CONSTRAINT ck_roles_default_scope
            CHECK (default_scope IN ('GLOBAL', 'ORGANIZATION', 'STORE'));
    END IF;
END $$;

-- Mark seeded roles
UPDATE roles SET system_group = TRUE, group_type = 'SYSTEM', allows_administration = TRUE, visual_priority = 10
WHERE code = 'ADMIN';
UPDATE roles SET system_group = TRUE, group_type = 'ADMINISTRATIVE', allows_administration = TRUE, visual_priority = 20
WHERE code = 'MANAGER';
UPDATE roles SET system_group = TRUE, group_type = 'OPERATIONAL', visual_priority = 50
WHERE code IN ('SELLER', 'STOCK_KEEPER');

CREATE TABLE group_permission_assignments (
    id                  UUID            NOT NULL,
    group_id            UUID            NOT NULL,
    permission_id       UUID            NOT NULL,
    grant_type          VARCHAR(10)     NOT NULL DEFAULT 'ALLOW',
    scope               VARCHAR(20)     NOT NULL DEFAULT 'ORGANIZATION',
    organization_id     UUID            NULL,
    store_id            UUID            NULL,
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_to            TIMESTAMPTZ     NULL,
    granted_by          UUID            NULL,
    reason              VARCHAR(500)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_group_permission_assignments PRIMARY KEY (id),
    CONSTRAINT uk_gpa_group_perm UNIQUE (group_id, permission_id),
    CONSTRAINT fk_gpa_group FOREIGN KEY (group_id) REFERENCES roles (id),
    CONSTRAINT fk_gpa_permission FOREIGN KEY (permission_id) REFERENCES permissions (id),
    CONSTRAINT fk_gpa_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_gpa_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_gpa_grant CHECK (grant_type IN ('ALLOW')),
    CONSTRAINT ck_gpa_scope CHECK (scope IN ('GLOBAL', 'ORGANIZATION', 'STORE')),
    CONSTRAINT ck_gpa_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE TABLE user_group_assignments (
    id                  UUID            NOT NULL,
    user_id             UUID            NOT NULL,
    group_id            UUID            NOT NULL,
    organization_id     UUID            NULL,
    store_id            UUID            NULL,
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_to            TIMESTAMPTZ     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    assigned_by         UUID            NULL,
    reason              VARCHAR(500)    NULL,
    primary_group       BOOLEAN         NOT NULL DEFAULT FALSE,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_group_assignments PRIMARY KEY (id),
    CONSTRAINT uk_uga_user_group_store UNIQUE NULLS NOT DISTINCT (user_id, group_id, store_id),
    CONSTRAINT fk_uga_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_uga_group FOREIGN KEY (group_id) REFERENCES roles (id),
    CONSTRAINT fk_uga_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_uga_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_uga_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

CREATE TABLE access_change_history (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    actor_user_id      UUID            NULL,
    target_user_id      UUID            NULL,
    group_id            UUID            NULL,
    permission_id       UUID            NULL,
    change_type         VARCHAR(40)     NOT NULL,
    details             TEXT            NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_access_change_history PRIMARY KEY (id)
);

CREATE TABLE access_policies (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(60)     NOT NULL,
    name                VARCHAR(120)    NOT NULL,
    description         VARCHAR(255)    NULL,
    default_group_id    UUID            NULL,
    require_store_context BOOLEAN       NOT NULL DEFAULT TRUE,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_access_policies PRIMARY KEY (id),
    CONSTRAINT uk_ap_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_ap_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ap_default_group FOREIGN KEY (default_group_id) REFERENCES roles (id)
);

-- Migrate role_permissions → group_permission_assignments
INSERT INTO group_permission_assignments (
    id, group_id, permission_id, grant_type, scope, valid_from, status, active, created_at, updated_at, version)
SELECT gen_random_uuid(), rp.role_id, rp.permission_id, 'ALLOW', 'ORGANIZATION',
       COALESCE(rp.created_at, NOW() AT TIME ZONE 'UTC'), 'ACTIVE', TRUE,
       COALESCE(rp.created_at, NOW() AT TIME ZONE 'UTC'), NOW() AT TIME ZONE 'UTC', 0
FROM role_permissions rp
ON CONFLICT (group_id, permission_id) DO NOTHING;

-- Migrate user_roles → user_group_assignments
INSERT INTO user_group_assignments (
    id, user_id, group_id, valid_from, status, primary_group, active, created_at, updated_at, version)
SELECT gen_random_uuid(), ur.user_id, ur.role_id,
       COALESCE(ur.created_at, NOW() AT TIME ZONE 'UTC'), 'ACTIVE', FALSE, TRUE,
       COALESCE(ur.created_at, NOW() AT TIME ZONE 'UTC'), NOW() AT TIME ZONE 'UTC', 0
FROM user_roles ur
ON CONFLICT (user_id, group_id, store_id) DO NOTHING;

CREATE INDEX idx_gpa_group ON group_permission_assignments (group_id);
CREATE INDEX idx_uga_user ON user_group_assignments (user_id);
CREATE INDEX idx_uga_group ON user_group_assignments (group_id);
CREATE INDEX idx_ach_occurred ON access_change_history (occurred_at DESC);
