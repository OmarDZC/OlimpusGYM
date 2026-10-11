package com.gymflow.config;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración pública mínima que el frontend necesita antes de iniciar sesión (Trozo 8a-2):
 * de momento, solo si este despliegue es la demo pública (perfil "demo"), para mostrar los
 * botones de "entrar como demo" y la franja de aviso en {@code /acceso}. En modo "prod" (o
 * "dev") {@code demoMode} es {@code false} y el frontend no los muestra.
 */
@RestController
@RequestMapping("/api/v1/public/config")
public class PublicConfigController {

    private final Environment environment;

    public PublicConfigController(Environment environment) {
        this.environment = environment;
    }

    public record PublicConfig(boolean demoMode) {
    }

    @GetMapping
    public PublicConfig config() {
        return new PublicConfig(environment.matchesProfiles("demo"));
    }
}
