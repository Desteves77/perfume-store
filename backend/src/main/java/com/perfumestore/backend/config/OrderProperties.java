package com.perfumestore.backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.order")
public class OrderProperties {
    private Duration paymentTtl = Duration.ofMinutes(15);
    private Duration expirationInterval = Duration.ofMinutes(1);
    private int expirationBatchSize = 100;
    private boolean expirationEnabled = true;
    public Duration getPaymentTtl() { return paymentTtl; }
    public void setPaymentTtl(Duration value) { this.paymentTtl = value; }
    public Duration getExpirationInterval() { return expirationInterval; }
    public void setExpirationInterval(Duration value) { this.expirationInterval = value; }
    public int getExpirationBatchSize() { return expirationBatchSize; }
    public void setExpirationBatchSize(int value) { this.expirationBatchSize = value; }
    public boolean isExpirationEnabled() { return expirationEnabled; }
    public void setExpirationEnabled(boolean value) { this.expirationEnabled = value; }
    public void validate() {
        if (paymentTtl == null || paymentTtl.isZero() || paymentTtl.isNegative()
                || expirationInterval == null || expirationInterval.isZero() || expirationInterval.isNegative()
                || expirationBatchSize < 1 || expirationBatchSize > 1000) {
            throw new IllegalStateException("Configuração de expiração de pedidos inválida");
        }
    }
}
