package com.perfumestore.backend.payment;

import com.perfumestore.backend.order.OrderController;
import com.perfumestore.backend.order.OrderDtos;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Profile({"local", "test"})
@RestController
@RequestMapping("/api/v1/dev/payment")
public class PaymentSimulationController {
    private final PaymentService payments;
    public PaymentSimulationController(PaymentService payments) { this.payments = payments; }
    @PostMapping("/orders/{publicNumber}")
    public OrderDtos.OrderResponse simulate(@PathVariable String publicNumber,
            @RequestBody OrderDtos.SimulatePaymentRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return payments.simulate(OrderController.customer(authentication), publicNumber,
                request == null || request.outcome() == null ? null : request.outcome().name(), correlation);
    }
}
