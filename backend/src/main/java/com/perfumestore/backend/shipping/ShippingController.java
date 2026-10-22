package com.perfumestore.backend.shipping;

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.shared.api.ApiRequestException;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipping")
public class ShippingController {

    private final ShippingService shipping;

    public ShippingController(ShippingService shipping) {
        this.shipping = shipping;
    }

    @PostMapping("/quotes")
    public ShippingDtos.QuoteResponse quote(@Valid @RequestBody ShippingDtos.QuoteRequest request,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ApiRequestException(401, "AUTHENTICATION_REQUIRED",
                    "É necessário autenticar para consultar o frete.");
        }
        return shipping.quote(principal.id(), request.addressId());
    }
}
