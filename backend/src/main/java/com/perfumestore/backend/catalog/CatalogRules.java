package com.perfumestore.backend.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import com.perfumestore.backend.shared.api.ApiRequestException;

final class CatalogRules {

    private CatalogRules() { }

    static String text(String value) {
        return value == null ? null : value.trim();
    }

    static String slug(String value) {
        return text(value).toLowerCase(Locale.ROOT);
    }

    static String sku(String value) {
        return text(value).toUpperCase(Locale.ROOT);
    }

    static BigDecimal scaleMoney(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() > 2) {
            throw new ApiRequestException(422, "INVALID_MONEY_SCALE",
                    "O preço deve ser positivo e ter no máximo duas casas decimais.");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    static BigDecimal scaleVolume(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() > 2) {
            throw new ApiRequestException(422, "INVALID_VOLUME",
                    "O volume deve ser positivo e ter no máximo duas casas decimais.");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    static ApiRequestException conflict(String message) {
        return new ApiRequestException(409, "CATALOG_CONFLICT", message);
    }
}
