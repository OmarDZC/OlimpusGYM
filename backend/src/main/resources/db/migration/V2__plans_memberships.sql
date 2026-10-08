-- OlympusGYM - planes y membresías (Trozo 1: autenticación y roles)

CREATE TABLE plans (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID         NOT NULL REFERENCES tenants (id),
    code                 VARCHAR(20)  NOT NULL CHECK (code IN ('BASICA', 'COMPLETA', 'ATLETA_PRO')),
    name                 VARCHAR(60)  NOT NULL,
    price_cents          INTEGER      NOT NULL,
    classes_per_month    INTEGER,                 -- NULL = ilimitadas
    booking_window_days  INTEGER      NOT NULL,
    ai_trainer           BOOLEAN      NOT NULL DEFAULT false,
    ai_assistant         BOOLEAN      NOT NULL DEFAULT false,
    active               BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT uq_plans_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_plans_tenant ON plans (tenant_id);

CREATE TABLE memberships (
    id          UUID PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenants (id),
    user_id     UUID        NOT NULL REFERENCES users (id),
    plan_id     UUID        NOT NULL REFERENCES plans (id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    renews_at   TIMESTAMPTZ
);

CREATE INDEX idx_memberships_tenant ON memberships (tenant_id);
CREATE INDEX idx_memberships_user ON memberships (user_id);
