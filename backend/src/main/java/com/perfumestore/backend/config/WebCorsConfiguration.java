package com.perfumestore.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties({ CorsProperties.class, CsrfCookieProperties.class, RateLimitProperties.class,
        PasswordRecoveryProperties.class, SessionCookieProperties.class, AdminProvisioningProperties.class })
public class WebCorsConfiguration {

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        if (properties.isAllowCredentials() && properties.getAllowedOrigins().contains("*")) {
            throw new IllegalStateException("CORS com credenciais não pode usar origem curinga");
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getAllowedOrigins());
        configuration.setAllowedMethods(properties.getAllowedMethods());
        configuration.setAllowedHeaders(properties.getAllowedHeaders());
        configuration.setExposedHeaders(properties.getExposedHeaders());
        configuration.setAllowCredentials(properties.isAllowCredentials());
        configuration.setMaxAge(properties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
