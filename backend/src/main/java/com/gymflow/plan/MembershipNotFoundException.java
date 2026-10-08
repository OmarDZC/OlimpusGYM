package com.gymflow.plan;

public class MembershipNotFoundException extends RuntimeException {

    public MembershipNotFoundException() {
        super("No tienes una cuota de socio activa.");
    }
}
