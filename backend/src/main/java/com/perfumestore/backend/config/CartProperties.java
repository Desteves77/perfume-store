package com.perfumestore.backend.config;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cart")
public class CartProperties {

    private Duration visitorTtl = Duration.ofDays(30);
    private Duration customerTtl = Duration.ofDays(90);
    private Duration cleanupRetention = Duration.ofDays(7);
    private Duration cleanupInterval = Duration.ofHours(1);
    private int cleanupBatchSize = 100;
    private int maxItems = 50;
    private int maxQuantityPerItem = 20;
    private boolean cleanupEnabled = true;

    public Duration getVisitorTtl() { return visitorTtl; }
    public void setVisitorTtl(Duration visitorTtl) { this.visitorTtl = visitorTtl; }
    public Duration getCustomerTtl() { return customerTtl; }
    public void setCustomerTtl(Duration customerTtl) { this.customerTtl = customerTtl; }
    public Duration getCleanupRetention() { return cleanupRetention; }
    public void setCleanupRetention(Duration cleanupRetention) { this.cleanupRetention = cleanupRetention; }
    public Duration getCleanupInterval() { return cleanupInterval; }
    public void setCleanupInterval(Duration cleanupInterval) { this.cleanupInterval = cleanupInterval; }
    public int getCleanupBatchSize() { return cleanupBatchSize; }
    public void setCleanupBatchSize(int cleanupBatchSize) { this.cleanupBatchSize = cleanupBatchSize; }
    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
    public int getMaxQuantityPerItem() { return maxQuantityPerItem; }
    public void setMaxQuantityPerItem(int maxQuantityPerItem) { this.maxQuantityPerItem = maxQuantityPerItem; }
    public boolean isCleanupEnabled() { return cleanupEnabled; }
    public void setCleanupEnabled(boolean cleanupEnabled) { this.cleanupEnabled = cleanupEnabled; }

    @PostConstruct
    void validateAtStartup() {
        validate();
    }

    public void validate() {
        if (visitorTtl == null || visitorTtl.isNegative() || visitorTtl.isZero()
                || customerTtl == null || customerTtl.isNegative() || customerTtl.isZero()
                || cleanupRetention == null || cleanupRetention.isNegative()
                || cleanupInterval == null || cleanupInterval.isNegative() || cleanupInterval.isZero()
                || cleanupBatchSize < 1 || cleanupBatchSize > 10_000
                || maxItems < 1 || maxItems > 500
                || maxQuantityPerItem < 1 || maxQuantityPerItem > 1_000) {
            throw new IllegalStateException("Configuração de carrinho inválida.");
        }
    }
}
