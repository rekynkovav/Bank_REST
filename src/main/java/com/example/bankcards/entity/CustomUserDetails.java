package com.example.bankcards.entity;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {

    private final Long id;
    private final String email;

    public CustomUserDetails(
            Long id,
            String username,
            String password,
            String email,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, authorities);
        this.id = id;
        this.email = email;
    }

    public CustomUserDetails(
            Long id,
            String username,
            String password,
            String email,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, enabled, true, true, true, authorities);
        this.id = id;
        this.email = email;
    }
}