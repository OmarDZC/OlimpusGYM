package com.gymflow.config;

import com.gymflow.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resuelve el tenant de cada petición. Provisional: lee la cabecera X-Tenant-Id.
 * TODO (Fase 1): derivar el tenant del JWT y no fiarse nunca de una cabecera del cliente.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER);
            if (header != null && !header.isBlank()) {
                try {
                    TenantContext.set(UUID.fromString(header.trim()));
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-Id no es un UUID válido");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
