package com.gymflow.admin;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder mínimo del panel de gestión: solo sirve para comprobar que los
 * permisos por rol funcionan (LOGIC.md §2). El contenido real (resumen, clases,
 * socios) llega en el Trozo 4.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "area", "admin");
    }

    @GetMapping("/owner/ping")
    public Map<String, String> ownerPing() {
        return Map.of("status", "ok", "area", "owner");
    }
}
