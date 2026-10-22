package com.perfumestore.backend.shipping;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.address.AddressDtos;
import com.perfumestore.backend.address.AddressService;
import com.perfumestore.backend.config.ShippingProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingService {

    private final AddressService addresses;
    private final ShippingQuoteRepository quotes;
    private final ShippingProperties properties;

    public ShippingService(AddressService addresses, ShippingQuoteRepository quotes, ShippingProperties properties) {
        this.addresses = addresses;
        this.quotes = quotes;
        this.properties = properties;
    }

    @Transactional
    public ShippingDtos.QuoteResponse quote(UUID userId, UUID addressId) {
        try {
            properties.validateForQuote();
        } catch (IllegalStateException exception) {
            throw new ApiRequestException(503, "SHIPPING_UNCONFIGURED",
                    "A regra comercial de frete não está configurada para este ambiente.");
        }
        AddressDtos.AddressData address = addresses.requireOwned(userId, addressId);
        long postalCode = Long.parseLong(address.postalCode());
        boolean served = properties.validatedRanges().stream()
                .anyMatch(range -> postalCode >= range.from() && postalCode <= range.to());
        if (!served) {
            throw new ApiRequestException(422, "SHIPPING_DESTINATION_UNSERVED",
                    "O destino informado não está dentro da área de atendimento.");
        }
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(properties.getQuoteTtl());
        UUID quoteId = UUID.randomUUID();
        quotes.insert(quoteId, userId, address.id(), address.postalCode(), address.city(), address.state(),
                properties.getMethod(), properties.getAmount().setScale(2, RoundingMode.UNNECESSARY),
                properties.getMinDays(), properties.getMaxDays(), properties.getRuleVersion(), expiresAt, createdAt);
        return new ShippingDtos.QuoteResponse(quoteId, true, properties.getMethod(),
                properties.getAmount().setScale(2, RoundingMode.UNNECESSARY), "BRL",
                new ShippingDtos.EstimatedDelivery(properties.getMinDays(), properties.getMaxDays(), "BUSINESS"),
                properties.getRuleVersion(), expiresAt);
    }
}
