package com.perfumestore.backend.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpirationScheduler {
    private final OrderService orders;
    public OrderExpirationScheduler(OrderService orders) { this.orders = orders; }
    @Scheduled(fixedDelayString = "${ORDER_EXPIRATION_INTERVAL_MS:60000}")
    public void expire() { orders.expireDue("scheduler"); }
}
