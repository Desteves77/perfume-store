package com.perfumestore.backend.payment;

import java.util.UUID;

import com.perfumestore.backend.order.AdminOrderController;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Profile({"local", "test"})
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class MockRefundController {
    private final RefundService refunds;
    public MockRefundController(RefundService refunds) { this.refunds = refunds; }

    @PostMapping("/api/v1/dev/payment/refunds/{refundId}/simulate")
    public RefundDtos.Detail simulate(@PathVariable UUID refundId, @RequestBody RefundDtos.SimulateRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return refunds.simulate(refundId, request == null ? null : request.outcome(), AdminOrderController.admin(authentication), correlation);
    }

    @PostMapping("/api/v1/dev/payment/refunds/{refundId}/reconcile")
    public RefundDtos.Detail reconcile(@PathVariable UUID refundId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return refunds.reconcile(refundId, AdminOrderController.admin(authentication), correlation);
    }
}
