-- V149: profissionais (Employee) distintos de User
CREATE TABLE employees (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    registration_number     VARCHAR(40)     NOT NULL,
    name                    VARCHAR(200)    NOT NULL,
    social_name             VARCHAR(200)    NULL,
    cpf                     VARCHAR(11)     NULL,
    rg                      VARCHAR(30)     NULL,
    birth_date              DATE            NULL,
    email                   VARCHAR(255)    NULL,
    phone                   VARCHAR(30)     NULL,
    mobile                  VARCHAR(30)     NULL,
    admission_date          DATE            NULL,
    termination_date        DATE            NULL,
    job_title               VARCHAR(120)    NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    user_id                 UUID            NULL,
    can_sell                BOOLEAN         NOT NULL DEFAULT TRUE,
    notes                   VARCHAR(2000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT fk_employees_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_employees_organization_registration UNIQUE (organization_id, registration_number),
    CONSTRAINT uk_employees_user UNIQUE (user_id),
    CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED')),
    CONSTRAINT ck_employees_registration_not_blank CHECK (LENGTH(TRIM(registration_number)) > 0),
    CONSTRAINT ck_employees_termination_after_admission CHECK (
        termination_date IS NULL OR admission_date IS NULL OR termination_date >= admission_date
    )
);

CREATE UNIQUE INDEX uk_employees_cpf
    ON employees (cpf)
    WHERE cpf IS NOT NULL AND LENGTH(TRIM(cpf)) > 0;

CREATE INDEX idx_employees_organization ON employees (organization_id);
CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_job_title ON employees (job_title);
CREATE INDEX idx_employees_name ON employees (name);
CREATE INDEX idx_employees_user ON employees (user_id);
CREATE INDEX idx_employees_active ON employees (active);

COMMENT ON TABLE employees IS 'Profissionais da organização (pessoa operacional; distinto de User)';
COMMENT ON COLUMN employees.registration_number IS 'Matrícula única por organização';
COMMENT ON COLUMN employees.user_id IS 'Credencial opcional; um User só pode vincular a um Employee';
COMMENT ON COLUMN employees.can_sell IS 'Habilitado a figurar como vendedor em vendas';

-- Seed: profissional padrão (sem usuário; admin permanece conta técnica)
INSERT INTO employees (
    id, organization_id, registration_number, name, social_name, job_title, status,
    can_sell, admission_date, active, created_at, updated_at, version
)
SELECT
    'b2000000-0000-4000-8000-000000000001',
    o.id,
    'EMP-0001',
    'Colaborador Padrão',
    NULL,
    'Vendedor',
    'ACTIVE',
    TRUE,
    CURRENT_DATE,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM organizations o
WHERE o.code = 'ORG-DEFAULT'
ON CONFLICT (id) DO NOTHING;
