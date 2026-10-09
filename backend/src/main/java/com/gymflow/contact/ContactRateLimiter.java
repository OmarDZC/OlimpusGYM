package com.gymflow.contact;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Límite de envíos por IP (Trozo 7a, LOGIC.md §6), en memoria: suficiente para una sola
 * instancia del backend, que es lo que hay en este MVP. Si algún día hay varias instancias,
 * esto tendría que pasar a Redis para compartir el contador entre ellas.
 */
@Component
public class ContactRateLimiter {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Clock clock;
    private final Map<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    public ContactRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** {@code true} si se permite el envío (y queda registrado); {@code false} si supera el límite. */
    public synchronized boolean allow(String ip) {
        Instant now = clock.instant();
        Deque<Instant> attempts = attemptsByIp.computeIfAbsent(ip, key -> new ArrayDeque<>());
        Instant windowStart = now.minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(windowStart)) {
            attempts.pollFirst();
        }
        if (attempts.size() >= MAX_ATTEMPTS) {
            return false;
        }
        attempts.addLast(now);
        return true;
    }
}
