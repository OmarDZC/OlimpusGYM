package com.gymflow.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Busca por correo en todo el sistema. Hay un único login para todos los tenants
     * y, en la práctica, un único tenant real (olympusgym); la unicidad de (tenant_id,
     * email) de la tabla "users" evita colisiones dentro de un mismo gimnasio.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);
}
