-- V284: Permissões sensíveis, sessões, auditoria ACL, revisão periódica (Prompts 162–166)

ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS risk_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS requires_justification BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS requires_dual_approval BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sensitive BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_permissions_risk_level') THEN
        ALTER TABLE permissions ADD CONSTRAINT ck_permissions_risk_level
            CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));
    END IF;
END $$;

UPDATE permissions SET risk_level = 'CRITICAL', sensitive = TRUE, requires_justification = TRUE, requires_dual_approval = TRUE
WHERE code IN (
    'USER_CREATE', 'USER_UPDATE', 'USER_DELETE', 'ACCESS_GROUP_CREATE', 'ACCESS_GROUP_UPDATE',
    'ACCESS_GROUP_PERMISSION_MANAGE', 'ACCESS_GROUP_MEMBER_MANAGE', 'ACCESS_GROUP_DISABLE',
    'GLOBAL_STORE_ACCESS', 'HIERARCHY_MANAGE', 'EFFECTIVE_PERMISSION_READ'
) OR code LIKE '%_PAY' OR code LIKE '%_REVERSE' OR code LIKE '%_CANCEL'
   OR code LIKE 'FISCAL_%CERTIFICATE%' OR code LIKE '%_REOPEN%'
   OR code LIKE '%BANK%BALANCE%' OR code LIKE '%EXPORT%' OR code LIKE '%ANONYMI%';

UPDATE permissions SET risk_level = 'HIGH', sensitive = TRUE, requires_justification = TRUE
WHERE risk_level = 'MEDIUM' AND (
    code LIKE '%_DELETE' OR code LIKE '%_DISABLE' OR code LIKE '%_AUTHORIZE%'
    OR code LIKE 'FISCAL_%' OR code LIKE 'FINANCIAL_%'
);

CREATE TABLE IF NOT EXISTS privileged_access_requests (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    requester_user_id   UUID            NOT NULL,
    target_group_id     UUID            NOT NULL,
    permission_id       UUID            NOT NULL,
    justification       VARCHAR(1000)   NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    valid_to            TIMESTAMPTZ     NULL,
    decided_at          TIMESTAMPTZ     NULL,
    decided_by          UUID            NULL,
    decision_reason     VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_privileged_access_requests PRIMARY KEY (id),
    CONSTRAINT fk_par_requester FOREIGN KEY (requester_user_id) REFERENCES users (id),
    CONSTRAINT fk_par_group FOREIGN KEY (target_group_id) REFERENCES roles (id),
    CONSTRAINT fk_par_permission FOREIGN KEY (permission_id) REFERENCES permissions (id),
    CONSTRAINT ck_par_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS privileged_access_approvals (
    id                  UUID            NOT NULL,
    request_id          UUID            NOT NULL,
    approver_user_id    UUID            NOT NULL,
    decision            VARCHAR(20)     NOT NULL,
    reason              VARCHAR(500)    NULL,
    decided_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_privileged_access_approvals PRIMARY KEY (id),
    CONSTRAINT uk_paa_request_approver UNIQUE (request_id, approver_user_id),
    CONSTRAINT fk_paa_request FOREIGN KEY (request_id) REFERENCES privileged_access_requests (id),
    CONSTRAINT fk_paa_approver FOREIGN KEY (approver_user_id) REFERENCES users (id),
    CONSTRAINT ck_paa_decision CHECK (decision IN ('APPROVED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id                  UUID            NOT NULL,
    user_id             UUID            NOT NULL,
    organization_id     UUID            NULL,
    store_id            UUID            NULL,
    access_version      BIGINT          NOT NULL DEFAULT 0,
    refresh_token_id    UUID            NULL,
    ip_address          VARCHAR(64)     NULL,
    user_agent          VARCHAR(500)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    started_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    last_seen_at        TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ended_at            TIMESTAMPTZ     NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_sessions PRIMARY KEY (id),
    CONSTRAINT fk_us_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_us_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'LOGGED_OUT'))
);

CREATE TABLE IF NOT EXISTS session_revocations (
    id                  UUID            NOT NULL,
    session_id          UUID            NOT NULL,
    revoked_by          UUID            NULL,
    reason              VARCHAR(500)    NULL,
    revoked_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_session_revocations PRIMARY KEY (id),
    CONSTRAINT fk_sr_session FOREIGN KEY (session_id) REFERENCES user_sessions (id)
);

CREATE TABLE IF NOT EXISTS permission_cache_versions (
    id                  UUID            NOT NULL,
    cache_key           VARCHAR(80)     NOT NULL,
    version_value       BIGINT          NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_permission_cache_versions PRIMARY KEY (id),
    CONSTRAINT uk_pcv_key UNIQUE (cache_key)
);

INSERT INTO permission_cache_versions (id, cache_key, version_value, updated_at)
VALUES ('e1000000-0000-4000-8000-000000000001', 'GLOBAL', 0, NOW() AT TIME ZONE 'UTC')
ON CONFLICT (cache_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS access_audit_events (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    store_id            UUID            NULL,
    actor_user_id       UUID            NULL,
    target_user_id      UUID            NULL,
    group_id            UUID            NULL,
    permission_id       UUID            NULL,
    permission_code     VARCHAR(80)     NULL,
    scope               VARCHAR(30)     NULL,
    event_type          VARCHAR(60)     NOT NULL,
    result              VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS',
    reason              VARCHAR(500)    NULL,
    before_data         TEXT            NULL,
    after_data          TEXT            NULL,
    ip_address          VARCHAR(64)     NULL,
    correlation_id      VARCHAR(64)     NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_access_audit_events PRIMARY KEY (id),
    CONSTRAINT ck_aae_result CHECK (result IN ('SUCCESS', 'DENIED', 'FAILURE'))
);

CREATE INDEX IF NOT EXISTS idx_aae_occurred ON access_audit_events (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_aae_actor ON access_audit_events (actor_user_id);
CREATE INDEX IF NOT EXISTS idx_aae_type ON access_audit_events (event_type);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user ON user_sessions (user_id);

CREATE TABLE IF NOT EXISTS access_reviews (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    code                VARCHAR(40)     NOT NULL,
    title               VARCHAR(150)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    reviewer_user_id    UUID            NULL,
    scheduled_at        TIMESTAMPTZ     NULL,
    completed_at        TIMESTAMPTZ     NULL,
    next_review_at      TIMESTAMPTZ     NULL,
    notes               VARCHAR(1000)   NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_access_reviews PRIMARY KEY (id),
    CONSTRAINT uk_access_reviews_code UNIQUE (code),
    CONSTRAINT ck_ar_status CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS access_review_items (
    id                  UUID            NOT NULL,
    review_id           UUID            NOT NULL,
    user_id             UUID            NULL,
    group_id            UUID            NULL,
    decision            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    decided_by          UUID            NULL,
    decided_at          TIMESTAMPTZ     NULL,
    notes               VARCHAR(500)    NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_access_review_items PRIMARY KEY (id),
    CONSTRAINT fk_ari_review FOREIGN KEY (review_id) REFERENCES access_reviews (id),
    CONSTRAINT ck_ari_decision CHECK (decision IN ('PENDING', 'MAINTAIN', 'REMOVE', 'MODIFY'))
);

CREATE TABLE IF NOT EXISTS access_review_decisions (
    id                  UUID            NOT NULL,
    review_item_id      UUID            NOT NULL,
    decision            VARCHAR(20)     NOT NULL,
    decided_by          UUID            NOT NULL,
    reason              VARCHAR(500)    NULL,
    decided_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_access_review_decisions PRIMARY KEY (id),
    CONSTRAINT fk_ard_item FOREIGN KEY (review_item_id) REFERENCES access_review_items (id)
);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version,
                         system_permission, code_immutable, risk_level, sensitive, requires_justification, requires_dual_approval)
VALUES
    ('a1000000-0000-4000-8000-000000000392', 'PRIVILEGED_ACCESS_REQUEST', 'Solicitar acesso privilegiado', 'ACCESS',
     'Solicitar concessão de permissões críticas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE, 'HIGH', TRUE, FALSE, FALSE),
    ('a1000000-0000-4000-8000-000000000393', 'PRIVILEGED_ACCESS_APPROVE', 'Aprovar acesso privilegiado', 'ACCESS',
     'Aprovar/rejeitar concessões críticas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE, 'CRITICAL', TRUE, TRUE, FALSE),
    ('a1000000-0000-4000-8000-000000000394', 'ACCESS_AUDIT_READ', 'Consultar auditoria de acesso', 'ACCESS',
     'Consultar eventos de auditoria ACL', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE, 'HIGH', TRUE, FALSE, FALSE),
    ('a1000000-0000-4000-8000-000000000395', 'ACCESS_REVIEW_MANAGE', 'Gerenciar revisão de acessos', 'ACCESS',
     'Criar e concluir revisões periódicas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE, 'HIGH', TRUE, TRUE, FALSE),
    ('a1000000-0000-4000-8000-000000000396', 'ACCESS_REPORT_READ', 'Consultar relatórios de acesso', 'ACCESS',
     'Relatórios de usuários, grupos e permissões', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE, 'MEDIUM', FALSE, FALSE, FALSE),
    ('a1000000-0000-4000-8000-000000000397', 'SESSION_MANAGE', 'Gerenciar sessões', 'ACCESS',
     'Consultar e revogar sessões de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE, 'HIGH', TRUE, FALSE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'PRIVILEGED_ACCESS_REQUEST', 'PRIVILEGED_ACCESS_APPROVE', 'ACCESS_AUDIT_READ',
      'ACCESS_REVIEW_MANAGE', 'ACCESS_REPORT_READ', 'SESSION_MANAGE')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO group_permission_assignments (
    id, group_id, permission_id, grant_type, scope, valid_from, status, active, created_at, updated_at, version)
SELECT gen_random_uuid(), r.id, p.id, 'ALLOW', 'ORGANIZATION', NOW() AT TIME ZONE 'UTC', 'ACTIVE', TRUE,
       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'PRIVILEGED_ACCESS_REQUEST', 'PRIVILEGED_ACCESS_APPROVE', 'ACCESS_AUDIT_READ',
      'ACCESS_REVIEW_MANAGE', 'ACCESS_REPORT_READ', 'SESSION_MANAGE')
ON CONFLICT (group_id, permission_id) DO NOTHING;

-- Grupo de contingência administrativa (Prompt 167)
INSERT INTO roles (id, code, name, description, active, created_at, updated_at, version,
                   group_type, system_group, default_group, default_scope, allows_administration, visual_priority)
SELECT 'a2000000-0000-4000-8000-000000000099', 'ADMIN_CONTINGENCY', 'Contingência administrativa',
       'Grupo de contingência — não remover sem validação', TRUE,
       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0,
       'SYSTEM', TRUE, FALSE, 'ORGANIZATION', TRUE, 5
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN_CONTINGENCY');

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN_CONTINGENCY'
  AND p.code IN ('USER_READ', 'USER_CREATE', 'USER_UPDATE', 'ACCESS_GROUP_READ', 'ACCESS_GROUP_UPDATE',
                 'ACCESS_GROUP_PERMISSION_MANAGE', 'ACCESS_GROUP_MEMBER_MANAGE', 'GLOBAL_STORE_ACCESS')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
