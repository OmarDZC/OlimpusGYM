-- OlympusGYM - sesiones de clase y reservas (Trozo 2: horario y reservas)

CREATE TABLE class_sessions (
    id             UUID PRIMARY KEY,
    tenant_id      UUID         NOT NULL REFERENCES tenants (id),
    name           VARCHAR(120) NOT NULL,
    discipline     VARCHAR(60)  NOT NULL,
    description    TEXT,
    coach_user_id  UUID         REFERENCES users (id),
    room           VARCHAR(60)  NOT NULL,
    starts_at      TIMESTAMPTZ  NOT NULL,
    ends_at        TIMESTAMPTZ  NOT NULL,
    capacity       INTEGER      NOT NULL CHECK (capacity > 0),
    status         VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'CANCELLED'))
);

CREATE INDEX idx_class_sessions_tenant ON class_sessions (tenant_id);
CREATE INDEX idx_class_sessions_starts_at ON class_sessions (starts_at);

CREATE TABLE bookings (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID        NOT NULL REFERENCES tenants (id),
    session_id         UUID        NOT NULL REFERENCES class_sessions (id),
    user_id            UUID        NOT NULL REFERENCES users (id),
    status             VARCHAR(20) NOT NULL CHECK (status IN ('BOOKED', 'WAITLIST', 'CANCELLED', 'ATTENDED', 'NO_SHOW')),
    waitlist_position  INTEGER,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at       TIMESTAMPTZ
);

CREATE INDEX idx_bookings_tenant ON bookings (tenant_id);
CREATE INDEX idx_bookings_session ON bookings (session_id);
CREATE INDEX idx_bookings_user ON bookings (user_id);

-- Un socio solo puede tener una reserva activa (BOOKED o WAITLIST) por sesión (LOGIC.md §4).
CREATE UNIQUE INDEX uq_bookings_active_per_user_session
    ON bookings (session_id, user_id)
    WHERE status IN ('BOOKED', 'WAITLIST');
