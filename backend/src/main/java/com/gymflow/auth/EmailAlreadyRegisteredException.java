package com.gymflow.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Ya existe una cuenta con el correo " + email);
    }
}
