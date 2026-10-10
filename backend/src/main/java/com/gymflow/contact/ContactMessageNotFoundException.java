package com.gymflow.contact;

/** Mensaje de contacto inexistente, o de otro tenant (Trozo 7b). */
public class ContactMessageNotFoundException extends RuntimeException {

    public ContactMessageNotFoundException() {
        super("No existe ese mensaje");
    }
}
