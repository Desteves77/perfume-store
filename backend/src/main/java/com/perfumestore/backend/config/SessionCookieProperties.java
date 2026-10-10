package com.perfumestore.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.session-cookie")
public class SessionCookieProperties {

    private boolean secure;
    private String sameSite = "Lax";

    public boolean isSecure() { return secure; }
    public void setSecure(boolean value) { secure = value; }
    public String getSameSite() { return sameSite; }
    public void setSameSite(String value) { sameSite = value; }
}
