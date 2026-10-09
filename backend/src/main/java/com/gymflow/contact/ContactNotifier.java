package com.gymflow.contact;

/**
 * Aviso de un mensaje nuevo de contacto (Trozo 7a, LOGIC.md §8). Misma idea que
 * {@code booking.Notifier}: única implementación del MVP escribe en log; email/SMTP se
 * añadiría implementando esta misma interfaz, sin tocar {@link ContactService}.
 */
public interface ContactNotifier {

    void received(ContactMessage message);
}
