package com.perfumestore.backend.order;

import java.time.Instant;
import java.util.UUID;

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final AdminOrderService orders;

    public AdminOrderController(AdminOrderService orders) { this.orders = orders; }

    @GetMapping
    public PageResponse<AdminOrderDtos.Summary> list(@Valid PageQuery page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return orders.list(page, search, status, paymentStatus, deliveryStatus, from, to);
    }

    @GetMapping("/{publicNumber}")
    public AdminOrderDtos.Detail detail(@PathVariable String publicNumber) { return orders.detail(publicNumber); }

    @PostMapping("/{publicNumber}/prepare")
    public ResponseEntity<AdminOrderDtos.Detail> prepare(@PathVariable String publicNumber,
            @Valid @RequestBody(required = false) AdminOrderDtos.PrepareRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        AdminOrderDtos.PrepareRequest body = request == null ? new AdminOrderDtos.PrepareRequest(null) : request;
        return ResponseEntity.ok(orders.prepare(publicNumber, key, admin(authentication), body.reason(), correlation));
    }

    @PostMapping("/{publicNumber}/ship")
    public ResponseEntity<AdminOrderDtos.Detail> ship(@PathVariable String publicNumber,
            @Valid @RequestBody AdminOrderDtos.ShipRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return ResponseEntity.ok(orders.ship(publicNumber, key, admin(authentication), request, correlation));
    }

    @PostMapping("/{publicNumber}/deliver")
    public ResponseEntity<AdminOrderDtos.Detail> deliver(@PathVariable String publicNumber,
            @Valid @RequestBody(required = false) AdminOrderDtos.DeliverRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        AdminOrderDtos.DeliverRequest body = request == null ? new AdminOrderDtos.DeliverRequest(null) : request;
        return ResponseEntity.ok(orders.deliver(publicNumber, key, admin(authentication), body.reason(), correlation));
    }

    @PostMapping("/{publicNumber}/cancel")
    public ResponseEntity<AdminOrderDtos.Detail> cancel(@PathVariable String publicNumber,
            @Valid @RequestBody AdminOrderDtos.CancelRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return ResponseEntity.ok(orders.cancel(publicNumber, key, admin(authentication), request.reason(), correlation));
    }

    public static UUID admin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)
                || !"ADMIN".equals(principal.role())) {
            throw new ApiRequestException(403, "FORBIDDEN", "A operaÃ§Ã£o exige perfil ADMIN.");
        }
        return principal.id();
    }
}
