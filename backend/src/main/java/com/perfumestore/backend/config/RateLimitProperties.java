package com.perfumestore.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rate-limit")
public class RateLimitProperties {

    private int loginMaxAttempts = 5;
    private Duration loginWindow = Duration.ofMinutes(1);
    private int recoveryMaxAttempts = 5;
    private Duration recoveryWindow = Duration.ofMinutes(15);
    private int resetMaxAttempts = 5;
    private Duration resetWindow = Duration.ofMinutes(15);
    private int registerMaxAttempts = 10;
    private Duration registerWindow = Duration.ofMinutes(10);

    public int getLoginMaxAttempts() { return loginMaxAttempts; }
    public void setLoginMaxAttempts(int value) { loginMaxAttempts = value; }
    public Duration getLoginWindow() { return loginWindow; }
    public void setLoginWindow(Duration value) { loginWindow = value; }
    public int getRecoveryMaxAttempts() { return recoveryMaxAttempts; }
    public void setRecoveryMaxAttempts(int value) { recoveryMaxAttempts = value; }
    public Duration getRecoveryWindow() { return recoveryWindow; }
    public void setRecoveryWindow(Duration value) { recoveryWindow = value; }
    public int getResetMaxAttempts() { return resetMaxAttempts; }
    public void setResetMaxAttempts(int value) { resetMaxAttempts = value; }
    public Duration getResetWindow() { return resetWindow; }
    public void setResetWindow(Duration value) { resetWindow = value; }
    public int getRegisterMaxAttempts() { return registerMaxAttempts; }
    public void setRegisterMaxAttempts(int value) { registerMaxAttempts = value; }
    public Duration getRegisterWindow() { return registerWindow; }
    public void setRegisterWindow(Duration value) { registerWindow = value; }
}
