package com.perfumestore.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {
    private boolean enabled;
    private boolean dispatchEnabled;
    private int batchSize = 20;
    private int maxAttempts = 5;
    private Duration initialBackoff = Duration.ofMinutes(1);
    private Duration maxBackoff = Duration.ofHours(1);
    private Duration leaseDuration = Duration.ofMinutes(5);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isDispatchEnabled() { return dispatchEnabled; }
    public void setDispatchEnabled(boolean dispatchEnabled) { this.dispatchEnabled = dispatchEnabled; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Duration getInitialBackoff() { return initialBackoff; }
    public void setInitialBackoff(Duration initialBackoff) { this.initialBackoff = initialBackoff; }
    public Duration getMaxBackoff() { return maxBackoff; }
    public void setMaxBackoff(Duration maxBackoff) { this.maxBackoff = maxBackoff; }
    public Duration getLeaseDuration() { return leaseDuration; }
    public void setLeaseDuration(Duration leaseDuration) { this.leaseDuration = leaseDuration; }

    public void validate() {
        if (batchSize < 1 || batchSize > 100 || maxAttempts < 1 || maxAttempts > 20
                || initialBackoff == null || initialBackoff.isNegative() || initialBackoff.isZero()
                || maxBackoff == null || maxBackoff.compareTo(initialBackoff) < 0
                || leaseDuration == null || leaseDuration.isNegative() || leaseDuration.isZero()) {
            throw new IllegalStateException("Configuracao de notificacoes invalida");
        }
    }
}
