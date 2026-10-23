package com.perfumestore.backend.order;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ReturnController {
    private final ReturnService returns;
    public ReturnController(ReturnService returns) { this.returns = returns; }

    @GetMapping("/api/v1/admin/returns")
    public PageResponse<ReturnDtos.Summary> list(@Valid PageQuery page, @RequestParam(required = false) ReturnStatus status) { return returns.list(page, status); }

    @GetMapping("/api/v1/admin/returns/{returnId}")
    public ReturnDtos.Detail get(@PathVariable UUID returnId) { return returns.get(returnId); }

    @PostMapping("/api/v1/admin/orders/{publicNumber}/returns")
    public ResponseEntity<ReturnDtos.Detail> request(@PathVariable String publicNumber,
            @Valid @RequestBody ReturnDtos.CreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            Authentication authentication) {
        return ResponseEntity.ok(returns.request(publicNumber, request, AdminOrderController.admin(authentication), key));
    }

    @PostMapping("/api/v1/admin/returns/{returnId}/receive")
    public ResponseEntity<ReturnDtos.Detail> receive(@PathVariable UUID returnId,
            @Valid @RequestBody ReturnDtos.ReceiveRequest request, Authentication authentication) {
        return ResponseEntity.ok(returns.receive(returnId, request, AdminOrderController.admin(authentication)));
    }

    @PostMapping("/api/v1/admin/returns/{returnId}/inspect")
    public ResponseEntity<ReturnDtos.Detail> inspect(@PathVariable UUID returnId,
            @Valid @RequestBody ReturnDtos.InspectRequest request, Authentication authentication) {
        return ResponseEntity.ok(returns.inspect(returnId, request, AdminOrderController.admin(authentication)));
    }

    @PostMapping("/api/v1/admin/returns/{returnId}/restock")
    public ResponseEntity<ReturnDtos.Detail> restock(@PathVariable UUID returnId,
            @Valid @RequestBody ReturnDtos.RestockRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) {
        return ResponseEntity.ok(returns.restock(returnId, request, AdminOrderController.admin(authentication), key, correlation));
    }
}
