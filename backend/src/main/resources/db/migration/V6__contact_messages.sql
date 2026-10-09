CREATE TABLE contact_messages (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL REFERENCES tenants (id),
    name       VARCHAR(120) NOT NULL,
    email      VARCHAR(180) NOT NULL,
    message    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_contact_messages_tenant ON contact_messages (tenant_id);
