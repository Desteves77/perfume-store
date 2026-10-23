package com.perfumestore.backend.cart;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.catalog.CatalogService;
import com.perfumestore.backend.config.CartCookieProperties;
import com.perfumestore.backend.config.CartProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.inventory.InventoryAvailabilityReader;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CART_TOKEN_HEADER = "CART_TOKEN";

    private final CartRepository carts;
    private final CatalogService catalog;
    private final InventoryAvailabilityReader availability;
    private final CartProperties properties;
    private final CartCookieProperties cookieProperties;

    public CartService(CartRepository carts, CatalogService catalog, InventoryAvailabilityReader availability,
            CartProperties properties, CartCookieProperties cookieProperties) {
        this.carts = carts;
        this.catalog = catalog;
        this.availability = availability;
        this.properties = properties;
        this.cookieProperties = cookieProperties;
    }

    @Transactional
    public CartDtos.CartResponse get(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        UUID userId = userId(authentication);
        CartRecord cart;
        if (userId == null && visitorToken(request) == null) {
            return CartDtos.CartResponse.empty();
        }
        if (userId != null) {
            claimVisitorCartInternal(userId, visitorToken(request), response);
            cart = ensureCustomerCart(userId);
        } else {
            cart = findGuestCart(visitorToken(request), response, false);
            if (cart == null) {
                return CartDtos.CartResponse.empty();
            }
        }
        touch(cart, userId != null);
        return render(cart.id());
    }

    @Transactional
    public CartDtos.CartResponse add(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response, CartDtos.AddItemRequest input, String operationKey) {
        UUID userId = userId(authentication);
        CartRecord cart;
        if (userId != null) {
            claimVisitorCartInternal(userId, visitorToken(request), response);
            cart = ensureCustomerCart(userId);
        } else {
            cart = findGuestCart(visitorToken(request), response, true);
        }
        if (operationKey != null && !operationKey.isBlank()) {
            validateOperationKey(operationKey);
            String fingerprint = fingerprint(input.presentationId() + ":" + input.quantity());
            CartMutationKeyRecord previous = carts.findMutationKey(cart.id(), operationKey).orElse(null);
            if (previous != null) {
                if (!previous.fingerprint().equals(fingerprint)) {
                    throw conflict("IDEMPOTENCY_KEY_REUSED", "A chave de operação foi usada com dados diferentes.");
                }
                return render(cart.id());
            }
            if (!carts.insertMutationKey(UUID.randomUUID(), cart.id(), operationKey, fingerprint, Instant.now())) {
                CartMutationKeyRecord concurrent = carts.findMutationKey(cart.id(), operationKey).orElseThrow();
                if (!concurrent.fingerprint().equals(fingerprint)) {
                    throw conflict("IDEMPOTENCY_KEY_REUSED", "A chave de operação foi usada com dados diferentes.");
                }
                return render(cart.id());
            }
        }
        CatalogService.CartPresentation presentation = catalog.cartPresentation(input.presentationId())
                .orElseThrow(() -> notFound("PRESENTATION_NOT_FOUND", "A apresentação não foi encontrada."));
        if (!presentation.published()) {
            throw conflict("PRESENTATION_NOT_ELIGIBLE", "A apresentação não está disponível no catálogo publicado.");
        }
        CartItemRecord existing = carts.findItemForUpdate(cart.id(), input.presentationId()).orElse(null);
        int quantity;
        try {
            quantity = Math.addExact(existing == null ? 0 : existing.quantity(), input.quantity());
        } catch (ArithmeticException exception) {
            throw conflict("QUANTITY_LIMIT_EXCEEDED", "A quantidade informada ultrapassa o limite técnico.");
        }
        if (quantity > properties.getMaxQuantityPerItem()) {
            throw conflict("QUANTITY_LIMIT_EXCEEDED", "A quantidade informada ultrapassa o limite técnico.");
        }
        Instant now = Instant.now();
        if (existing == null) {
            if (carts.countItems(cart.id()) >= properties.getMaxItems()) {
                throw conflict("CART_ITEM_LIMIT_EXCEEDED", "O carrinho atingiu o limite técnico de itens.");
            }
            carts.insertItem(UUID.randomUUID(), cart.id(), input.presentationId(), quantity, presentation.price(), now);
        } else {
            carts.updateItem(cart.id(), input.presentationId(), quantity, existing.priceReference(), existing.version(), now)
                    .orElseThrow(() -> conflict("CONCURRENT_UPDATE", "O item foi alterado. Consulte o carrinho e tente novamente."));
        }
        touch(cart, userId != null);
        return render(cart.id());
    }

    @Transactional
    public CartDtos.CartResponse update(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response, UUID presentationId, CartDtos.UpdateItemRequest input) {
        CartRecord cart = requireCart(authentication, request, response);
        CatalogService.CartPresentation presentation = catalog.cartPresentation(presentationId)
                .orElseThrow(() -> notFound("PRESENTATION_NOT_FOUND", "A apresentação não foi encontrada."));
        CartItemRecord current = carts.findItemForUpdate(cart.id(), presentationId)
                .orElseThrow(() -> notFound("CART_ITEM_NOT_FOUND", "O item não foi encontrado no carrinho."));
        if (input.quantity() > properties.getMaxQuantityPerItem()) {
            throw conflict("QUANTITY_LIMIT_EXCEEDED", "A quantidade informada ultrapassa o limite técnico.");
        }
        carts.updateItem(cart.id(), presentationId, input.quantity(), current.priceReference(), input.version(), Instant.now())
                .orElseThrow(() -> conflict("CONCURRENT_UPDATE", "O item foi alterado. Consulte o carrinho e tente novamente."));
        touch(cart, userId(authentication) != null);
        return render(cart.id());
    }

    @Transactional
    public void remove(Authentication authentication, HttpServletRequest request, HttpServletResponse response,
            UUID presentationId) {
        CartRecord cart = findCartForRequest(authentication, request, response, false);
        if (cart == null) {
            return;
        }
        carts.findByIdForUpdate(cart.id()).orElseThrow();
        carts.deleteItem(cart.id(), presentationId);
        touch(cart, userId(authentication) != null);
    }

    @Transactional
    public CartDtos.CartResponse claim(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        UUID userId = requireUser(authentication);
        claimVisitorCartInternal(userId, visitorToken(request), response);
        return render(ensureCustomerCart(userId).id());
    }

    @Transactional
    public void claimVisitorCart(UUID userId, HttpServletRequest request, HttpServletResponse response) {
        claimVisitorCartInternal(userId, visitorToken(request), response);
    }

    public void clearVisitorCookie(HttpServletResponse response) {
        clearCookie(response);
    }

    @Transactional
    public int cleanup() {
        properties.validate();
        Instant now = Instant.now();
        int expired = carts.markExpiredBatch(now, properties.getCleanupBatchSize());
        return expired + carts.deleteCleaned(now.minus(properties.getCleanupRetention()), properties.getCleanupBatchSize());
    }

    private CartRecord requireCart(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        CartRecord cart = findCartForRequest(authentication, request, response, false);
        if (cart == null) {
            throw notFound("CART_NOT_FOUND", "O carrinho não foi encontrado.");
        }
        return carts.findByIdForUpdate(cart.id()).orElseThrow();
    }

    private CartRecord findCartForRequest(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response, boolean createGuest) {
        UUID userId = userId(authentication);
        if (userId != null) {
            claimVisitorCartInternal(userId, visitorToken(request), response);
            return ensureCustomerCart(userId);
        }
        return findGuestCart(visitorToken(request), response, createGuest);
    }

    private CartRecord ensureCustomerCart(UUID userId) {
        Instant now = Instant.now();
        CartRecord existing = carts.findActiveByUserForUpdate(userId).orElse(null);
        if (existing != null) {
            if (!existing.expiresAt().isAfter(now)) {
                carts.markExpired(existing.id(), existing.version(), now);
            } else {
                return existing;
            }
        }
        return carts.insertCart(UUID.randomUUID(), userId, null, CartState.ACTIVE,
                now.plus(properties.getCustomerTtl()), now);
    }

    private CartRecord findGuestCart(String token, HttpServletResponse response, boolean create) {
        Instant now = Instant.now();
        if (token != null) {
            CartRecord existing = carts.findByTokenHashForUpdate(hash(token)).orElse(null);
            if (existing != null && existing.state() == CartState.ACTIVE && existing.userId() == null
                    && existing.expiresAt().isAfter(now)) {
                return existing;
            }
            if (existing != null && existing.state() == CartState.ACTIVE) {
                carts.markExpired(existing.id(), existing.version(), now);
            }
            clearCookie(response);
        }
        if (!create) {
            return null;
        }
        String freshToken = newToken();
        CartRecord created = carts.insertCart(UUID.randomUUID(), null, hash(freshToken), CartState.ACTIVE,
                now.plus(properties.getVisitorTtl()), now);
        writeCookie(response, freshToken);
        return created;
    }

    private void claimVisitorCartInternal(UUID userId, String token, HttpServletResponse response) {
        if (token == null) {
            return;
        }
        ensureCustomerCart(userId);
        CartRecord visitorCandidate = carts.findByTokenHash(hash(token)).orElse(null);
        if (visitorCandidate == null) {
            clearCookie(response);
            return;
        }
        CartRecord userCandidate = carts.findActiveByUser(userId).orElseThrow();
        List<UUID> ids = new ArrayList<>(List.of(visitorCandidate.id(), userCandidate.id()));
        ids.sort(Comparator.naturalOrder());
        Map<UUID, CartRecord> locked = new HashMap<>();
        ids.forEach(id -> carts.findByIdForUpdate(id).ifPresent(record -> locked.put(id, record)));
        CartRecord visitor = locked.get(visitorCandidate.id());
        CartRecord user = locked.get(userCandidate.id());
        if (visitor == null || user == null || visitor.state() != CartState.ACTIVE || visitor.userId() != null) {
            clearCookie(response);
            return;
        }
        Instant now = Instant.now();
        if (!visitor.expiresAt().isAfter(now)) {
            carts.markExpired(visitor.id(), visitor.version(), now);
            clearCookie(response);
            return;
        }
        Map<UUID, CartItemRecord> userItems = new HashMap<>();
        carts.findItems(user.id()).forEach(item -> userItems.put(item.presentationId(), item));
        for (CartItemRecord visitorItem : carts.findItems(visitor.id())) {
            CartItemRecord current = userItems.get(visitorItem.presentationId());
            if (current == null) {
                carts.insertItem(UUID.randomUUID(), user.id(), visitorItem.presentationId(), visitorItem.quantity(),
                        visitorItem.priceReference(), now);
            } else {
                int combined;
                try {
                    combined = Math.addExact(current.quantity(), visitorItem.quantity());
                } catch (ArithmeticException exception) {
                    throw conflict("QUANTITY_LIMIT_EXCEEDED", "A combinação ultrapassa o limite numérico suportado.");
                }
                carts.updateItem(user.id(), current.presentationId(), combined, current.priceReference(), current.version(), now)
                        .orElseThrow(() -> conflict("CONCURRENT_UPDATE", "O carrinho foi alterado durante a associação."));
            }
        }
        carts.markTransferred(visitor.id(), visitor.version(), userId, now);
        carts.updateVersion(user.id(), user.version(), now.plus(properties.getCustomerTtl()), now);
        clearCookie(response);
    }

    private void touch(CartRecord cart, boolean customer) {
        Instant now = Instant.now();
        carts.touch(cart.id(), now.plus(customer ? properties.getCustomerTtl() : properties.getVisitorTtl()), now);
    }

    private CartDtos.CartResponse render(UUID cartId) {
        CartRecord cart = carts.findByIdForUpdate(cartId).orElseThrow();
        List<CartItemRecord> items = carts.findItems(cartId);
        Map<UUID, Integer> stock = availability.availableByPresentationIds(items.stream()
                .map(CartItemRecord::presentationId).toList());
        List<CartDtos.ItemResponse> responses = new ArrayList<>();
        List<CartDtos.Issue> cartIssues = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        boolean canCheckout = !items.isEmpty();
        for (CartItemRecord item : items) {
            CatalogService.CartPresentation presentation = catalog.cartPresentation(item.presentationId()).orElse(null);
            List<CartDtos.Issue> issues = new ArrayList<>();
            MoneyResponse unitPrice = null;
            MoneyResponse lineTotal = null;
            UUID productId = null;
            String productName = null;
            String productSlug = null;
            String sku = null;
            BigDecimal volume = null;
            String unit = null;
            int available = stock.getOrDefault(item.presentationId(), 0);
            String status = available == 0 ? "OUT_OF_STOCK" : available < item.quantity() ? "INSUFFICIENT" : "AVAILABLE";
            if (presentation == null) {
                issues.add(issue("PRESENTATION_NOT_FOUND", "A apresentação não está mais disponível."));
            } else {
                productId = presentation.productId();
                productName = presentation.productName();
                productSlug = presentation.productSlug();
                sku = presentation.sku();
                volume = presentation.volume();
                unit = presentation.unit();
                unitPrice = new MoneyResponse(presentation.price(), presentation.currency());
                lineTotal = new MoneyResponse(presentation.price().multiply(BigDecimal.valueOf(item.quantity())),
                        presentation.currency());
                if (!presentation.published()) {
                    issues.add(issue("PRESENTATION_NOT_ELIGIBLE", "A apresentação não está publicada para compra."));
                }
                if (item.priceReference().compareTo(presentation.price()) != 0) {
                    issues.add(issue("PRICE_CHANGED", "O preço vigente da apresentação foi alterado."));
                }
                if (available < item.quantity()) {
                    issues.add(issue(available == 0 ? "OUT_OF_STOCK" : "QUANTITY_EXCEEDS_AVAILABLE",
                            available == 0 ? "A apresentação está sem disponibilidade." :
                                    "A quantidade solicitada excede a disponibilidade atual."));
                }
                if (item.quantity() > properties.getMaxQuantityPerItem()) {
                    issues.add(issue("QUANTITY_LIMIT_EXCEEDED", "A quantidade excede o limite técnico do carrinho."));
                }
                if (presentation.published()) {
                    subtotal = subtotal.add(lineTotal.amount());
                }
            }
            if (!issues.isEmpty()) {
                canCheckout = false;
            }
            responses.add(new CartDtos.ItemResponse(item.id(), item.presentationId(), productId, productName, productSlug,
                    sku, volume, unit, item.quantity(), unitPrice, lineTotal,
                    new CartDtos.Availability(status, available), List.copyOf(issues), item.version()));
        }
        if (items.size() > properties.getMaxItems()) {
            cartIssues.add(issue("CART_ITEM_LIMIT_EXCEEDED", "O carrinho excede o limite técnico de linhas."));
            canCheckout = false;
        }
        return new CartDtos.CartResponse(cart.id(), cart.state(), List.copyOf(responses),
                new MoneyResponse(subtotal, "BRL"), canCheckout && !responses.isEmpty(), List.copyOf(cartIssues),
                cart.version(), cart.expiresAt(), cart.lastActivityAt());
    }

    private CartDtos.Issue issue(String code, String message) {
        return new CartDtos.Issue(code, message);
    }

    private UUID userId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal
                ? principal.id() : null;
    }

    private UUID requireUser(Authentication authentication) {
        UUID id = userId(authentication);
        if (id == null) {
            throw new ApiRequestException(401, "AUTHENTICATION_REQUIRED", "É necessário autenticar para acessar este recurso.");
        }
        return id;
    }

    private String visitorToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieProperties.getName().equals(cookie.getName())) {
                String value = cookie.getValue();
                return value != null && value.matches("[A-Za-z0-9_-]{43,44}") ? value : null;
            }
        }
        return null;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private String fingerprint(String value) {
        return hash(value);
    }

    private void validateOperationKey(String key) {
        if (!key.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw new ApiRequestException(422, "IDEMPOTENCY_KEY_INVALID", "A chave de operação é inválida.");
        }
    }

    private void writeCookie(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", ResponseCookie.from(cookieProperties.getName(), token)
                .httpOnly(true).secure(cookieProperties.isSecure()).sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath()).maxAge(cookieProperties.getMaxAge()).build().toString());
    }

    private void clearCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", ResponseCookie.from(cookieProperties.getName(), "")
                .httpOnly(true).secure(cookieProperties.isSecure()).sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath()).maxAge(0).build().toString());
    }

    private ApiRequestException notFound(String code, String message) {
        return new ApiRequestException(404, code, message);
    }

    private ApiRequestException conflict(String code, String message) {
        return new ApiRequestException(409, code, message);
    }
}
