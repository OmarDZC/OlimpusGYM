package com.gymflow.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj inyectable en vez de {@code Instant.now()} suelto: así los tests pueden fijar "ahora"
 * (con {@code @MockitoBean Clock}) para probar de forma determinista reglas que dependen de la
 * hora (cancelar/reservar una clase ya empezada, próxima clase, historial...), sin depender del
 * reloj real de la máquina ni de condiciones de carrera por tiempo transcurrido entre pasos.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
