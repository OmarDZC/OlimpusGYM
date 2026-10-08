-- OlympusGYM - rutinas de entrenamiento (Trozo 3: mi cuenta)
-- El plan de la semana se guarda como JSON (texto, validado/serializado desde Java con
-- Jackson): es más simple que modelar cada ejercicio como filas para un MVP de una sola
-- rutina activa por socio y semana.

CREATE TABLE workout_plans (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants (id),
    user_id         UUID         NOT NULL REFERENCES users (id),
    week_start      DATE         NOT NULL,
    goal            VARCHAR(60)  NOT NULL,
    level           VARCHAR(30)  NOT NULL,
    days_per_week   INTEGER      NOT NULL,
    plan_json       TEXT         NOT NULL,
    source          VARCHAR(10)  NOT NULL CHECK (source IN ('AI', 'MANUAL')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_workout_plans_tenant_user ON workout_plans (tenant_id, user_id);

CREATE TABLE workout_logs (
    id               UUID PRIMARY KEY,
    tenant_id        UUID        NOT NULL REFERENCES tenants (id),
    user_id          UUID        NOT NULL REFERENCES users (id),
    workout_plan_id  UUID        NOT NULL REFERENCES workout_plans (id),
    day_index        INTEGER     NOT NULL,
    exercise_index   INTEGER     NOT NULL,
    set_index        INTEGER     NOT NULL,
    done             BOOLEAN     NOT NULL DEFAULT true,
    weight_kg        NUMERIC(6, 2),
    reps             INTEGER,
    logged_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workout_logs_set UNIQUE (workout_plan_id, day_index, exercise_index, set_index)
);

CREATE INDEX idx_workout_logs_tenant_user ON workout_logs (tenant_id, user_id);
