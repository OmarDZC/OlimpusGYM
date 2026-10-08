package com.gymflow.auth;

import com.gymflow.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resuelve la sesión a partir del JWT de la cabecera Authorization: Bearer.
 * Sustituye a la antigua cabecera provisional X-Tenant-Id (Trozo 1): el tenant
 * y el rol salen siempre del token firmado, nunca de algo que mande el cliente.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                String token = header.substring(BEARER_PREFIX.length());
                try {
                    JwtService.ParsedToken claims = jwtService.parse(token);
                    TenantContext.set(claims.tenantId());
                    var authority = new SimpleGrantedAuthority("ROLE_" + claims.role());
                    var authentication =
                        new UsernamePasswordAuthenticationToken(claims.userId(), null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (RuntimeException invalidToken) {
                    // Token ausente, caducado o manipulado: seguimos como anónimos.
                    // Spring Security decide si el recurso pedido exige autenticación.
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
