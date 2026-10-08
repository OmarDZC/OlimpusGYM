package com.gymflow.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    /**
     * MVP de un solo gimnasio: único tenant real, creado por seed (PROJECT_BRIEF.md).
     * No hay onboarding de gimnasios ni selector de tenant en el login.
     */
    public static final String DEFAULT_SLUG = "olympusgym";

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Tenant() {
    }

    public Tenant(String name, String slug) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
