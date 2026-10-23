package com.perfumestore.backend.order;

import java.util.UUID;
import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders = orders; }
    @PostMapping("/api/v1/checkout/orders")
    public ResponseEntity<OrderDtos.OrderResponse> create(@Valid @RequestBody OrderDtos.CheckoutRequest request,
            @RequestHeader("Idempotency-Key") String key, @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication authentication) { return ResponseEntity.status(HttpStatus.CREATED).body(orders.create(customer(authentication), request, key, correlation)); }
    @GetMapping("/api/v1/me/orders")
    public PageResponse<OrderDtos.OrderSummary> list(@Valid PageQuery page, Authentication authentication) { return orders.list(customer(authentication), page); }
    @GetMapping("/api/v1/me/orders/{publicNumber}")
    public OrderDtos.OrderResponse get(@PathVariable String publicNumber, Authentication authentication) { return orders.get(customer(authentication), publicNumber); }
    public static UUID customer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal p)) throw new ApiRequestException(401, "AUTHENTICATION_REQUIRED", "É necessário autenticar para acessar pedidos.");
        if (!"CUSTOMER".equals(p.role())) throw new ApiRequestException(403, "FORBIDDEN", "A operação exige perfil CUSTOMER.");
        return p.id();
    }
}
