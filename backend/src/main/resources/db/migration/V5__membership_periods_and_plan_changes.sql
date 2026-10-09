-- OlympusGYM - periodos de membresía y cambios de tarifa (Trozo 9b)
-- renews_at ya existía pero nunca se rellenaba ("Sin fecha fijada (demo)" en la pantalla).
-- Lo sustituye period_end, parte de un periodo real con inicio y fin, de un mes natural.

ALTER TABLE memberships
    ADD COLUMN period_start           TIMESTAMPTZ,
    ADD COLUMN period_end             TIMESTAMPTZ,
    ADD COLUMN pending_plan_id        UUID REFERENCES plans (id),
    ADD COLUMN renewal_notice_sent_at TIMESTAMPTZ;

-- Las membresías que ya existían (del seed, o de cualquier entorno que ya tuviera datos)
-- reciben un periodo real: un mes natural desde su alta.
UPDATE memberships
SET period_start = started_at,
    period_end   = started_at + INTERVAL '1 month'
WHERE period_start IS NULL;

ALTER TABLE memberships
    ALTER COLUMN period_start SET NOT NULL,
    ALTER COLUMN period_end SET NOT NULL;

ALTER TABLE memberships DROP COLUMN renews_at;

-- Historial de cambios de tarifa (quién, cuándo, de qué tarifa a cuál, importe): subidas al
-- instante, bajadas programadas/canceladas/aplicadas, y la renovación periódica normal.
CREATE TABLE membership_plan_changes (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenants (id),
    membership_id       UUID         NOT NULL REFERENCES memberships (id),
    -- NULL = lo hizo el job automático de renovación, no una persona.
    changed_by_user_id  UUID REFERENCES users (id),
    from_plan_id        UUID         NOT NULL REFERENCES plans (id),
    to_plan_id          UUID         NOT NULL REFERENCES plans (id),
    change_type         VARCHAR(30)  NOT NULL CHECK (change_type IN
                         ('UPGRADE', 'DOWNGRADE_SCHEDULED', 'DOWNGRADE_CANCELLED', 'DOWNGRADE_APPLIED', 'RENEWAL')),
    -- Céntimos enteros; NULL cuando el cambio no implica cobro (programar/cancelar una bajada).
    amount_cents        INTEGER,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_membership_plan_changes_membership ON membership_plan_changes (membership_id);
CREATE INDEX idx_membership_plan_changes_tenant ON membership_plan_changes (tenant_id);
