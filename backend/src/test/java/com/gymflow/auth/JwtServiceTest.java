package com.gymflow.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.ExpiredJwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** No necesita Docker ni Spring: corre siempre, también en local. */
class JwtServiceTest {

    private static final String SECRET = "change-me-change-me-change-me-change-me-32b";

    @Test
    void issuesAndParsesARoundTrippingToken() {
        JwtService jwtService = new JwtService(SECRET, 60);
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtService.issue(userId, tenantId, "MEMBER", "socio@olympusgym.test");
        JwtService.ParsedToken claims = jwtService.parse(token);

        assertEquals(userId, claims.userId());
        assertEquals(tenantId, claims.tenantId());
        assertEquals("MEMBER", claims.role());
        assertEquals("socio@olympusgym.test", claims.email());
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -1);
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID(), "OWNER", "owner@olympusgym.test");

        assertThrows(ExpiredJwtException.class, () -> jwtService.parse(token));
    }
}
