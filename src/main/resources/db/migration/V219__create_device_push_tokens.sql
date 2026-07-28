-- V219: Tokens de dispositivo mobile para push (Prompt 86)
CREATE TABLE device_push_tokens (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    user_id                 UUID            NOT NULL,
    platform                VARCHAR(20)     NOT NULL,
    token                   VARCHAR(500)    NOT NULL,
    device_name             VARCHAR(160)    NULL,
    app_version             VARCHAR(40)     NULL,
    last_seen_at            TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_device_push_tokens PRIMARY KEY (id),
    CONSTRAINT uk_dpt_token UNIQUE (token),
    CONSTRAINT fk_dpt_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_dpt_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_dpt_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

CREATE INDEX idx_dpt_user ON device_push_tokens (user_id) WHERE active = TRUE;

COMMENT ON TABLE device_push_tokens IS 'Registro de dispositivos para notificações push (Prompt 86); entrega FCM/APNs futura';
