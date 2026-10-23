package com.perfumestore.backend.payment;

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.order.OrderController;
import com.perfumestore.backend.order.OrderDtos;
import com.perfumestore.backend.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Profile({"local", "test"})
@RestController
@RequestMapping("/api/v1/dev/payment")
public class PaymentSimulationController {
    private final OrderService orders;
    public PaymentSimulationController(OrderService orders) { this.orders = orders; }
    @PostMapping("/orders/{publicNumber}")
    public OrderDtos.OrderResponse simulate(@PathVariable String publicNumber,
            @Valid @RequestBody OrderDtos.SimulatePaymentRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return orders.simulatePayment(OrderController.customer(authentication), publicNumber, request.outcome(), correlation);
    }
}
