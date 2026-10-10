package com.perfumestore.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-recovery")
public class PasswordRecoveryProperties {

    private Duration tokenTtl = Duration.ofMinutes(30);
    private String frontendBaseUrl;
    private String senderAddress;

    public Duration getTokenTtl() { return tokenTtl; }
    public void setTokenTtl(Duration value) { tokenTtl = value; }
    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String value) { frontendBaseUrl = value; }
    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String value) { senderAddress = value; }
}
