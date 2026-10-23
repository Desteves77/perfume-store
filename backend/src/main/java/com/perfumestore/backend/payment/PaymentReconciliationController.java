package com.perfumestore.backend.payment;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "test"})
@RestController
public class PaymentReconciliationController {
    private final PaymentService payments;

    public PaymentReconciliationController(PaymentService payments) { this.payments = payments; }

    @PostMapping("/api/v1/dev/payment/attempts/{paymentId}/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentDtos.ReconciliationResponse reconcile(@PathVariable UUID paymentId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return payments.reconcile(paymentId, correlation);
    }
}
