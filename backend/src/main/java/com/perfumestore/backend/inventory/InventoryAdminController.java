package com.perfumestore.backend.inventory;

import java.util.UUID;

import com.perfumestore.backend.shared.api.CorrelationIdFilter;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryAdminController {

    private final InventoryService inventory;

    public InventoryAdminController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/presentations/{presentationId}")
    public ResponseEntity<InventoryDtos.BalanceResponse> balance(@PathVariable UUID presentationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(inventory.balance(presentationId));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<InventoryDtos.BalanceResponse> adjust(@Valid @RequestBody InventoryDtos.AdjustmentRequest request,
            @RequestHeader("Idempotency-Key") String operationKey, Authentication authentication,
            HttpServletRequest servletRequest) {
        UUID actor = actorId(authentication);
        InventoryDtos.BalanceResponse result = inventory.adjust(request.presentationId(), request.quantity(), request.type(),
                request.reason(), operationKey, actor, CorrelationIdFilter.currentId(servletRequest));
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(result);
    }

    @GetMapping("/movements")
    public ResponseEntity<PageResponse<InventoryDtos.MovementResponse>> movements(@Valid @ModelAttribute PageQuery page,
            @RequestParam(required = false) UUID presentationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(inventory.movements(page, presentationId));
    }

    private UUID actorId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (RuntimeException exception) {
            throw new com.perfumestore.backend.shared.api.ApiRequestException(401, "ACTOR_REQUIRED",
                    "Não foi possível identificar o administrador da operação.");
        }
    }
}
