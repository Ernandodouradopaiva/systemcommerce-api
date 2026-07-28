-- V150: lotação histórica do profissional por loja
CREATE TABLE employee_store_assignments (
    id                      UUID            NOT NULL,
    employee_id             UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    assignment_type         VARCHAR(20)     NOT NULL,
    start_date              DATE            NOT NULL,
    end_date                DATE            NULL,
    primary_assignment      BOOLEAN         NOT NULL DEFAULT FALSE,
    store_role              VARCHAR(120)    NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    notes                   VARCHAR(2000)   NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_employee_store_assignments PRIMARY KEY (id),
    CONSTRAINT fk_esa_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_esa_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_esa_type CHECK (assignment_type IN ('PERMANENT', 'TEMPORARY', 'SUPPORT', 'SUBSTITUTE')),
    CONSTRAINT ck_esa_status CHECK (status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT ck_esa_period CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_esa_employee ON employee_store_assignments (employee_id);
CREATE INDEX idx_esa_store ON employee_store_assignments (store_id);
CREATE INDEX idx_esa_status ON employee_store_assignments (status);
CREATE INDEX idx_esa_primary ON employee_store_assignments (employee_id, primary_assignment)
    WHERE primary_assignment = TRUE AND status = 'ACTIVE';
CREATE INDEX idx_esa_period ON employee_store_assignments (employee_id, start_date, end_date);

COMMENT ON TABLE employee_store_assignments IS 'Lotação histórica do profissional por loja (não apagar; encerrar com end_date)';
COMMENT ON COLUMN employee_store_assignments.primary_assignment IS 'Lotação principal vigente (no máximo uma ativa por período)';

-- Seed: lotação permanente principal do EMP-0001 na LOJA-01
INSERT INTO employee_store_assignments (
    id, employee_id, store_id, assignment_type, start_date, end_date,
    primary_assignment, store_role, status, active, created_at, updated_at, version
)
SELECT
    'b2000000-0000-4000-8000-000000000002',
    e.id,
    s.id,
    'PERMANENT',
    CURRENT_DATE,
    NULL,
    TRUE,
    'Vendedor',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM employees e
JOIN stores s ON s.code = 'LOJA-01' AND s.organization_id = e.organization_id
WHERE e.registration_number = 'EMP-0001'
  AND NOT EXISTS (
      SELECT 1 FROM employee_store_assignments x WHERE x.id = 'b2000000-0000-4000-8000-000000000002'
  );
