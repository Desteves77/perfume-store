package com.perfumestore.backend.cart;

import com.perfumestore.backend.config.CartProperties;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CartCleanupScheduler {

    private final CartService carts;
    private final CartProperties properties;

    public CartCleanupScheduler(CartService carts, CartProperties properties) {
        this.carts = carts;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.cart.cleanup-interval:PT1H}")
    public void cleanup() {
        if (properties.isCleanupEnabled()) {
            carts.cleanup();
        }
    }
}
