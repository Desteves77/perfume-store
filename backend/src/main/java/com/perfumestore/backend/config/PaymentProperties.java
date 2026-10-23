package com.perfumestore.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {
    private boolean enabled;
    private String provider = "";
    private Duration checkoutTtl = Duration.ofMinutes(15);
    private Duration webhookTolerance = Duration.ofMinutes(5);
    private String mockWebhookSecret = "";
    private String mockCheckoutBaseUrl = "mock://checkout";
    private int reconciliationBatchSize = 100;
    private boolean reconciliationEnabled;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Duration getCheckoutTtl() { return checkoutTtl; }
    public void setCheckoutTtl(Duration checkoutTtl) { this.checkoutTtl = checkoutTtl; }
    public Duration getWebhookTolerance() { return webhookTolerance; }
    public void setWebhookTolerance(Duration webhookTolerance) { this.webhookTolerance = webhookTolerance; }
    public String getMockWebhookSecret() { return mockWebhookSecret; }
    public void setMockWebhookSecret(String mockWebhookSecret) { this.mockWebhookSecret = mockWebhookSecret; }
    public String getMockCheckoutBaseUrl() { return mockCheckoutBaseUrl; }
    public void setMockCheckoutBaseUrl(String mockCheckoutBaseUrl) { this.mockCheckoutBaseUrl = mockCheckoutBaseUrl; }
    public int getReconciliationBatchSize() { return reconciliationBatchSize; }
    public void setReconciliationBatchSize(int reconciliationBatchSize) { this.reconciliationBatchSize = reconciliationBatchSize; }
    public boolean isReconciliationEnabled() { return reconciliationEnabled; }
    public void setReconciliationEnabled(boolean reconciliationEnabled) { this.reconciliationEnabled = reconciliationEnabled; }

    public void validate() {
        if (!enabled) return;
        if (provider == null || provider.isBlank() || checkoutTtl == null || checkoutTtl.isNegative()
                || checkoutTtl.isZero() || webhookTolerance == null || webhookTolerance.isNegative()
                || webhookTolerance.isZero() || reconciliationBatchSize < 1 || reconciliationBatchSize > 1000) {
            throw new IllegalStateException("Configuração de pagamento inválida");
        }
        if ("MOCK".equals(provider) && (mockWebhookSecret == null || mockWebhookSecret.isBlank())) {
            throw new IllegalStateException("O segredo do webhook MOCK é obrigatório quando o provedor está habilitado");
        }
    }
}
