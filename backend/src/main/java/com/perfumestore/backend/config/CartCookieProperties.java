package com.perfumestore.backend.config;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cart.cookie")
public class CartCookieProperties {

    private String name = "CART_TOKEN";
    private String path = "/api/v1";
    private boolean secure;
    private String sameSite = "Lax";
    private Duration maxAge = Duration.ofDays(30);

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }
    public String getSameSite() { return sameSite; }
    public void setSameSite(String sameSite) { this.sameSite = sameSite; }
    public Duration getMaxAge() { return maxAge; }
    public void setMaxAge(Duration maxAge) { this.maxAge = maxAge; }

    @PostConstruct
    void validateAtStartup() {
        validate();
    }

    public void validate() {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_-]{2,50}")
                || path == null || !path.startsWith("/")
                || sameSite == null || !(sameSite.equalsIgnoreCase("Lax")
                        || sameSite.equalsIgnoreCase("Strict") || sameSite.equalsIgnoreCase("None"))
                || maxAge == null || maxAge.isNegative() || maxAge.isZero()
                || (sameSite.equalsIgnoreCase("None") && !secure)) {
            throw new IllegalStateException("Configuração do cookie do carrinho inválida.");
        }
    }
}
