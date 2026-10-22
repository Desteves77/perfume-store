package com.perfumestore.backend.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.shipping")
public class ShippingProperties {

    private boolean enabled;
    private String method;
    private String ruleVersion;
    private BigDecimal amount;
    private int minDays;
    private int maxDays;
    private Duration quoteTtl = Duration.ofMinutes(30);
    private boolean synthetic;
    private List<String> servedRanges = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getMinDays() {
        return minDays;
    }

    public void setMinDays(int minDays) {
        this.minDays = minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }

    public void setMaxDays(int maxDays) {
        this.maxDays = maxDays;
    }

    public Duration getQuoteTtl() {
        return quoteTtl;
    }

    public void setQuoteTtl(Duration quoteTtl) {
        this.quoteTtl = quoteTtl;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public void setSynthetic(boolean synthetic) {
        this.synthetic = synthetic;
    }

    public List<String> getServedRanges() {
        return servedRanges;
    }

    public void setServedRanges(List<String> servedRanges) {
        this.servedRanges = servedRanges == null ? List.of() : List.copyOf(servedRanges);
    }

    public List<PostalRange> validatedRanges() {
        if (servedRanges == null || servedRanges.isEmpty()) {
            throw new IllegalStateException("app.shipping.served-ranges deve ser informado");
        }
        List<PostalRange> ranges = servedRanges.stream().map(ShippingProperties::parseRange).sorted()
                .toList();
        for (int index = 1; index < ranges.size(); index++) {
            if (ranges.get(index - 1).to() >= ranges.get(index).from()) {
                throw new IllegalStateException("As faixas de CEP de frete não podem se sobrepor");
            }
        }
        return ranges;
    }

    public void validateForQuote() {
        if (!enabled) {
            throw new IllegalStateException("Regra de frete não habilitada");
        }
        if (method == null || method.isBlank() || ruleVersion == null || ruleVersion.isBlank() || amount == null
                || amount.signum() < 0 || amount.scale() > 2 || minDays < 0 || maxDays < minDays || quoteTtl == null
                || quoteTtl.isZero() || quoteTtl.isNegative()) {
            throw new IllegalStateException("Configuração de frete inválida");
        }
        validatedRanges();
    }

    private static PostalRange parseRange(String raw) {
        if (raw == null || !raw.matches("[0-9]{8}-[0-9]{8}")) {
            throw new IllegalStateException("Faixa de CEP inválida");
        }
        long from = Long.parseLong(raw.substring(0, 8));
        long to = Long.parseLong(raw.substring(9, 17));
        if (from > to) {
            throw new IllegalStateException("Limites da faixa de CEP estão invertidos");
        }
        return new PostalRange(from, to);
    }

    public record PostalRange(long from, long to) implements Comparable<PostalRange> {
        @Override
        public int compareTo(PostalRange other) {
            return Long.compare(from, other.from);
        }
    }
}
