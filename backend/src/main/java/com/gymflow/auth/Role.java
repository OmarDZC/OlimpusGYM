package com.gymflow.auth;

/** Roles de usuario (LOGIC.md §2). Un socio es MEMBER; STAFF y OWNER gestionan el gimnasio. */
public enum Role {
    OWNER,
    STAFF,
    MEMBER
}
