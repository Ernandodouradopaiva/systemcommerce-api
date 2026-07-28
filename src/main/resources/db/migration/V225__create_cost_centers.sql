-- V225: Centros de custo (Prompt 93)
CREATE TABLE cost_centers (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(2000)   NULL,
    parent_id           UUID            NULL,
    store_id            UUID            NULL,
    responsible_user_id UUID            NULL,
    accepts_posting     BOOLEAN         NOT NULL DEFAULT TRUE,
    valid_from          DATE            NULL,
    valid_until         DATE            NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    sort_order          INT             NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_cost_centers PRIMARY KEY (id),
    CONSTRAINT uk_cost_centers_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_cc_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_cc_parent FOREIGN KEY (parent_id) REFERENCES cost_centers (id),
    CONSTRAINT fk_cc_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_cc_responsible FOREIGN KEY (responsible_user_id) REFERENCES users (id),
    CONSTRAINT ck_cc_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_cc_org_store ON cost_centers (organization_id, store_id);
CREATE INDEX idx_cc_org_parent ON cost_centers (organization_id, parent_id);

CREATE TABLE cost_center_hierarchy (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    ancestor_id         UUID            NOT NULL,
    descendant_id       UUID            NOT NULL,
    depth               INT             NOT NULL,
    CONSTRAINT pk_cost_center_hierarchy PRIMARY KEY (id),
    CONSTRAINT uk_cch_pair UNIQUE (ancestor_id, descendant_id),
    CONSTRAINT fk_cch_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_cch_ancestor FOREIGN KEY (ancestor_id) REFERENCES cost_centers (id) ON DELETE CASCADE,
    CONSTRAINT fk_cch_descendant FOREIGN KEY (descendant_id) REFERENCES cost_centers (id) ON DELETE CASCADE,
    CONSTRAINT ck_cch_depth CHECK (depth >= 0)
);

CREATE INDEX idx_cch_descendant ON cost_center_hierarchy (descendant_id);

CREATE TABLE cost_center_store_assignments (
    id                  UUID            NOT NULL,
    cost_center_id      UUID            NOT NULL,
    store_id            UUID            NOT NULL,
    primary_assignment  BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_cost_center_store_assignments PRIMARY KEY (id),
    CONSTRAINT uk_ccsa_center_store UNIQUE (cost_center_id, store_id),
    CONSTRAINT fk_ccsa_center FOREIGN KEY (cost_center_id) REFERENCES cost_centers (id) ON DELETE CASCADE,
    CONSTRAINT fk_ccsa_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_ccsa_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Flag preparatória: conta financeira pode exigir centro de custo
ALTER TABLE financial_accounts
    ADD COLUMN requires_cost_center BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON TABLE cost_centers IS 'Centros de custo hierárquicos (Prompt 93) — global (store_id NULL) ou por loja';
COMMENT ON TABLE cost_center_store_assignments IS 'Vínculos adicionais centro × loja';
