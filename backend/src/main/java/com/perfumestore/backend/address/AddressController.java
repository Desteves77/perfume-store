package com.perfumestore.backend.address;

import java.util.UUID;

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/addresses")
@Validated
public class AddressController {

    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    public PageResponse<AddressDtos.AddressResponse> list(@Valid @ModelAttribute PageQuery query,
            Authentication authentication) {
        return addresses.list(principal(authentication).id(), query);
    }

    @PostMapping
    public ResponseEntity<AddressDtos.AddressResponse> create(@Valid @RequestBody AddressDtos.AddressRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(addresses.create(principal(authentication).id(), request));
    }

    @GetMapping("/{addressId}")
    public AddressDtos.AddressResponse get(@PathVariable UUID addressId, Authentication authentication) {
        return addresses.get(principal(authentication).id(), addressId);
    }

    @PatchMapping("/{addressId}")
    public AddressDtos.AddressResponse update(@PathVariable UUID addressId,
            @Valid @RequestBody AddressDtos.AddressUpdateRequest request, Authentication authentication) {
        return addresses.update(principal(authentication).id(), addressId, request);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable UUID addressId, Authentication authentication) {
        addresses.delete(principal(authentication).id(), addressId);
        return ResponseEntity.noContent().build();
    }

    private AppUserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ApiRequestException(401, "AUTHENTICATION_REQUIRED",
                    "É necessário autenticar para acessar este recurso.");
        }
        return principal;
    }
}
