package com.perfumestore.backend.cart;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Validated
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    public CartDtos.CartResponse get(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        return carts.get(authentication, request, response);
    }

    @PostMapping("/items")
    public CartDtos.CartResponse add(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response, @Valid @RequestBody CartDtos.AddItemRequest input,
            @RequestHeader(value = "Idempotency-Key", required = false) String operationKey) {
        return carts.add(authentication, request, response, input, operationKey);
    }

    @PatchMapping("/items/{presentationId}")
    public CartDtos.CartResponse update(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response, @PathVariable UUID presentationId,
            @Valid @RequestBody CartDtos.UpdateItemRequest input) {
        return carts.update(authentication, request, response, presentationId, input);
    }

    @DeleteMapping("/items/{presentationId}")
    public ResponseEntity<Void> remove(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response, @PathVariable UUID presentationId) {
        carts.remove(authentication, request, response, presentationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/claim")
    public CartDtos.CartResponse claim(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        return carts.claim(authentication, request, response);
    }
}
