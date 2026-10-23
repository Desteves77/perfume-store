package com.perfumestore.backend.payment;

import java.util.UUID;

import com.perfumestore.backend.order.AdminOrderController;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class RefundController {
    private final RefundService refunds;
    public RefundController(RefundService refunds) { this.refunds = refunds; }

    @GetMapping("/api/v1/admin/refunds")
    public PageResponse<RefundDtos.Summary> list(@Valid PageQuery page, @RequestParam(required = false) RefundStatus status) {
        return refunds.list(page, status);
    }

    @GetMapping("/api/v1/admin/refunds/{refundId}")
    public RefundDtos.Detail get(@PathVariable UUID refundId) { return refunds.get(refundId); }

    @PostMapping("/api/v1/admin/orders/{publicNumber}/refunds")
    public ResponseEntity<RefundDtos.Detail> request(@PathVariable String publicNumber,
            @Valid @RequestBody RefundDtos.CreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return ResponseEntity.ok(refunds.request(publicNumber, AdminOrderController.admin(authentication), request.reason(), key, correlation));
    }

}
