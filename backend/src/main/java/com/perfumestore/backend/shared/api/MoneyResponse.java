package com.perfumestore.backend.shared.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

import tools.jackson.databind.annotation.JsonSerialize;

public record MoneyResponse(
        @JsonSerialize(using = DecimalStringSerializer.class) BigDecimal amount,
        String currency) {

    public MoneyResponse {
        if (amount == null || currency == null || !"BRL".equals(currency)) {
            throw new IllegalArgumentException("MoneyResponse requires BRL");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
