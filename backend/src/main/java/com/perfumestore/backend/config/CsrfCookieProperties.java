package com.perfumestore.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.csrf")
public class CsrfCookieProperties {

    private boolean cookieSecure;
    private String cookieSameSite = "Lax";

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }
}
