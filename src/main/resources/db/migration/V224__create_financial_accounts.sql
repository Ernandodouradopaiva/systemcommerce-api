-- V224: Plano de contas e categorias financeiras (Prompt 92)
CREATE TABLE financial_accounts (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(2000)   NULL,
    parent_id           UUID            NULL,
    level_no            INT             NOT NULL DEFAULT 1,
    account_type        VARCHAR(20)     NOT NULL,
    nature              VARCHAR(20)     NOT NULL,
    accepts_posting     BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    sort_order          INT             NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_financial_accounts PRIMARY KEY (id),
    CONSTRAINT uk_financial_accounts_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_fa_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_fa_parent FOREIGN KEY (parent_id) REFERENCES financial_accounts (id),
    CONSTRAINT ck_fa_type CHECK (account_type IN ('REVENUE', 'EXPENSE', 'ASSET', 'LIABILITY', 'TRANSFER', 'ADJUSTMENT')),
    CONSTRAINT ck_fa_nature CHECK (nature IN ('CREDIT', 'DEBIT', 'NEUTRAL')),
    CONSTRAINT ck_fa_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_fa_level CHECK (level_no >= 1)
);

CREATE INDEX idx_fa_org_parent ON financial_accounts (organization_id, parent_id);
CREATE INDEX idx_fa_org_type ON financial_accounts (organization_id, account_type);
CREATE INDEX idx_fa_org_posting ON financial_accounts (organization_id, accepts_posting) WHERE accepts_posting = TRUE;

-- Closure / hierarquia materializada (ancestor → descendant)
CREATE TABLE financial_account_hierarchy (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    ancestor_id         UUID            NOT NULL,
    descendant_id       UUID            NOT NULL,
    depth               INT             NOT NULL,
    CONSTRAINT pk_financial_account_hierarchy PRIMARY KEY (id),
    CONSTRAINT uk_fah_pair UNIQUE (ancestor_id, descendant_id),
    CONSTRAINT fk_fah_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_fah_ancestor FOREIGN KEY (ancestor_id) REFERENCES financial_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_fah_descendant FOREIGN KEY (descendant_id) REFERENCES financial_accounts (id) ON DELETE CASCADE,
    CONSTRAINT ck_fah_depth CHECK (depth >= 0)
);

CREATE INDEX idx_fah_descendant ON financial_account_hierarchy (descendant_id);
CREATE INDEX idx_fah_ancestor ON financial_account_hierarchy (ancestor_id);

CREATE TABLE financial_categories (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(2000)   NULL,
    financial_account_id UUID           NULL,
    usage_scope         VARCHAR(20)     NOT NULL DEFAULT 'BOTH',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_financial_categories PRIMARY KEY (id),
    CONSTRAINT uk_fc_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_fc_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_fc_account FOREIGN KEY (financial_account_id) REFERENCES financial_accounts (id),
    CONSTRAINT ck_fc_scope CHECK (usage_scope IN ('PURCHASE', 'SALE', 'BOTH')),
    CONSTRAINT ck_fc_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_fc_org_scope ON financial_categories (organization_id, usage_scope);

COMMENT ON TABLE financial_accounts IS 'Plano de contas hierárquico (Prompt 92) — conta sintética não recebe lançamento';
COMMENT ON TABLE financial_account_hierarchy IS 'Closure table do plano de contas';
COMMENT ON TABLE financial_categories IS 'Categorias financeiras vinculadas a contas analíticas';
