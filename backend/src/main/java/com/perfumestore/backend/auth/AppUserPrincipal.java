package com.perfumestore.backend.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

import com.perfumestore.backend.user.AppUser;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AppUserPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String displayName;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean enabled;

    private AppUserPrincipal(AppUser user) {
        this.id = user.id();
        this.displayName = user.displayName();
        this.email = user.email();
        this.passwordHash = user.passwordHash();
        this.role = user.role().name();
        this.enabled = user.status().name().equals("ACTIVE");
    }

    public static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(user);
    }

    public UUID id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String email() {
        return email;
    }

    public String role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
