package com.perfumestore.backend.order;

import java.math.BigDecimal;
import java.util.UUID;

record OrderShippingRecord(UUID quoteId, String method, BigDecimal amount, String currency, int minDays, int maxDays,
        String dayType, String ruleVersion, String postalCode, String city, String state) { }
