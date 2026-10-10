package com.perfumestore.backend;

import com.perfumestore.backend.config.SecurityErrorHandlers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@TestConfiguration(proxyBeanMethods = false)
@Import(TestApiController.class)
public class TestApiConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain testApiSecurityFilterChain(HttpSecurity http, SecurityErrorHandlers handlers,
            CsrfTokenRepository csrfTokenRepository) throws Exception {
        http
                .securityMatcher("/test-api/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(requestCache -> requestCache.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/test-api/protected/**").authenticated()
                        .requestMatchers("/test-api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(handlers.authenticationEntryPoint())
                        .accessDeniedHandler(handlers.accessDeniedHandler()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
