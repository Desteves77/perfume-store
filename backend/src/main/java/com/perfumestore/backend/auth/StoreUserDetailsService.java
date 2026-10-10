package com.perfumestore.backend.auth;

import com.perfumestore.backend.user.EmailNormalizer;
import com.perfumestore.backend.user.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StoreUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public StoreUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByEmailNormalized(EmailNormalizer.normalize(username))
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
}
