package com.perfumestore.backend.payment;

import com.perfumestore.backend.order.OrderController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) { this.payments = payments; }

    @PostMapping("/api/v1/me/orders/{publicNumber}/payment-sessions")
    public ResponseEntity<PaymentDtos.PaymentResponse> create(@PathVariable String publicNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        PaymentService.PaymentCreateResult result = payments.createCheckout(OrderController.customer(authentication),
                publicNumber, idempotencyKey, correlation);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.response());
    }

    @GetMapping("/api/v1/me/orders/{publicNumber}/payment")
    public PaymentDtos.PaymentStatusResponse current(@PathVariable String publicNumber, Authentication authentication) {
        return payments.current(OrderController.customer(authentication), publicNumber);
    }

}
