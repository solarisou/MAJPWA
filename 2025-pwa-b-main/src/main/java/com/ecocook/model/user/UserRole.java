package com.ecocook.model.user;

import org.springframework.security.core.GrantedAuthority;

// on crée un enum UserRole qui représente les roles des utilisateurs
public enum UserRole implements GrantedAuthority {
    USER,
    MODERATEUR,
    ADMIN;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}

