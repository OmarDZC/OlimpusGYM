package com.gymflow.contact;

import com.gymflow.contact.dto.ContactRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Formulario de contacto público (Trozo 7a, LOGIC.md §6): sin sesión, con límite por IP. */
@RestController
@RequestMapping("/api/v1/public/contact")
public class PublicContactController {

    private final ContactService contactService;

    public PublicContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submit(@Valid @RequestBody ContactRequest request, HttpServletRequest httpRequest) {
        contactService.submit(request, clientIp(httpRequest));
    }

    /**
     * Sin proxy inverso delante en este MVP: la IP del socket ya es la del cliente. Si algún día
     * hay un balanceador o Caddy delante, esto tendría que leer {@code X-Forwarded-For} en su
     * lugar (y confiar solo en el proxy propio, nunca en una cabecera que manda el cliente).
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
