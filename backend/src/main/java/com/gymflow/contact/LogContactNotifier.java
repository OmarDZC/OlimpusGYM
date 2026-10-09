package com.gymflow.contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogContactNotifier implements ContactNotifier {

    private static final Logger log = LoggerFactory.getLogger(LogContactNotifier.class);

    @Override
    public void received(ContactMessage message) {
        log.info(
                "[notificacion] Mensaje de contacto nuevo: nombre={} correo={} id={}",
                message.getName(), message.getEmail(), message.getId());
    }
}
