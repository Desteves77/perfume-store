package com.perfumestore.backend.order;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.perfumestore.backend.address.AddressDtos;
import com.perfumestore.backend.address.AddressService;
import com.perfumestore.backend.cart.CartService;
import com.perfumestore.backend.catalog.CatalogService;
import com.perfumestore.backend.config.OrderProperties;
import com.perfumestore.backend.inventory.InventoryService;
import com.perfumestore.backend.inventory.InventoryDtos;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.shared.api.SortParser;
import com.perfumestore.backend.shared.inventory.InventoryAvailabilityReader;
import com.perfumestore.backend.shipping.ShippingQuoteRecord;
import com.perfumestore.backend.shipping.ShippingService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Map<String, String> SORTS = Map.of("id", "id", "createdAt", "created_at", "total", "total", "status", "order_status");
    private final OrderRepository orders;
    private final CartService carts;
    private final CatalogService catalog;
    private final InventoryAvailabilityReader availability;
    private final InventoryService inventory;
    private final AddressService addresses;
    private final ShippingService shipping;
    private final OrderProperties properties;

    public OrderService(OrderRepository orders, CartService carts, CatalogService catalog,
            InventoryAvailabilityReader availability, InventoryService inventory, AddressService addresses,
            ShippingService shipping, OrderProperties properties) {
        this.orders = orders; this.carts = carts; this.catalog = catalog; this.availability = availability;
        this.inventory = inventory; this.addresses = addresses; this.shipping = shipping; this.properties = properties;
    }

    @Transactional
    public OrderDtos.OrderResponse create(UUID userId, OrderDtos.CheckoutRequest request, String idempotencyKey,
            String correlationId) {
        validateKey(idempotencyKey);
        String fingerprint = digest(request.addressId() + "|" + request.quoteId());
        OrderRecord previous = orders.findByUserAndKey(userId, idempotencyKey).orElse(null);
        if (previous != null) return existingOrConflict(previous, fingerprint);
        properties.validate();
        Instant now = Instant.now();
        Instant expires = now.plus(properties.getPaymentTtl());
        UUID orderId = UUID.randomUUID();
        OrderRecord shell = new OrderRecord(orderId, publicNumber(), userId, null, idempotencyKey, fingerprint,
                OrderStatus.PENDING_PAYMENT, PaymentStatus.PENDING, DeliveryStatus.NOT_STARTED, BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), "BRL", now, now, expires);
        if (!orders.insertIfAbsent(shell)) {
            OrderRecord concurrent = orders.findByUserAndKey(userId, idempotencyKey).orElseThrow();
            return existingOrConflict(concurrent, fingerprint);
        }
        CartService.CheckoutCart cart = carts.checkoutSnapshot(userId);
        AddressDtos.AddressSnapshot address = addresses.requireOwnedSnapshot(userId, request.addressId());
        ShippingQuoteRecord quote = shipping.requireValidQuote(userId, request.quoteId(), request.addressId());
        List<CartLine> lines = new ArrayList<>();
        for (CartService.CheckoutItem item : cart.items()) {
            CatalogService.CartPresentation p = catalog.cartPresentation(item.presentationId()).orElseThrow(() ->
                    conflict("PRESENTATION_NOT_ELIGIBLE", "Uma apresentação do carrinho não está mais disponível."));
            if (!p.published()) throw conflict("PRESENTATION_NOT_ELIGIBLE", "Uma apresentação do carrinho não está publicada.");
            if (item.quantity() <= 0) throw conflict("CART_ITEM_INVALID", "O carrinho contém uma quantidade inválida.");
            BigDecimal lineTotal = p.price().multiply(BigDecimal.valueOf(item.quantity())).setScale(2);
            lines.add(new CartLine(item.presentationId(), item.quantity(), p, lineTotal));
        }
        Map<UUID, Integer> available = availability.availableByPresentationIds(lines.stream().map(CartLine::presentationId).toList());
        for (CartLine line : lines) if (available.getOrDefault(line.presentationId(), 0) < line.quantity())
            throw conflict("INSUFFICIENT_STOCK", "A disponibilidade de uma apresentação foi insuficiente.");
        lines.sort(Comparator.comparing(CartLine::presentationId));
        BigDecimal subtotal = lines.stream().map(CartLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        BigDecimal freight = quote.amount().setScale(2);
        BigDecimal total = subtotal.add(freight).setScale(2);
        OrderRecord order = new OrderRecord(shell.id(), shell.publicNumber(), userId, cart.id(), idempotencyKey, fingerprint,
                OrderStatus.PENDING_PAYMENT, PaymentStatus.PENDING, DeliveryStatus.NOT_STARTED, subtotal, freight, total,
                "BRL", shell.createdAt(), now, expires);
        orders.updateCommercial(order.id(), cart.id(), subtotal, freight, total, now, expires);
        orders.insertAddress(orderId, new OrderAddressRecord(address.recipientName(), address.label(), address.postalCode(),
                address.street(), address.number(), address.complement(), address.neighborhood(), address.city(),
                address.state(), address.reference()));
        orders.insertShipping(orderId, new OrderShippingRecord(quote.id(), quote.method(), quote.amount(), quote.currency(),
                quote.minDays(), quote.maxDays(), "BUSINESS", quote.ruleVersion(), quote.postalCode(), quote.city(), quote.state()));
        for (CartLine line : lines) {
            orders.insertItem(UUID.randomUUID(), orderId, new OrderItemRecord(UUID.randomUUID(), line.presentation().productId(),
                    line.presentationId(), line.presentation().sku(), line.presentation().productName(), line.presentation().volume(),
                    line.presentation().unit(), line.quantity(), line.presentation().price(), line.lineTotal(), "BRL"));
        }
        for (CartLine line : lines) {
            String key = reservationKey(orderId, line.presentationId());
            InventoryDtos.ReservationResponse reservation = inventory.reserve(line.presentationId(), line.quantity(), key,
                    orderId, expires, null, correlationId);
            orders.insertReservation(UUID.randomUUID(), orderId,
                    new OrderReservationRecord(reservation.id(), key, line.presentationId(), line.quantity()), now);
        }
        orders.insertHistory(UUID.randomUUID(), orderId, null, OrderStatus.PENDING_PAYMENT.name(), "CREATED", userId,
                correlationId, now);
        carts.clearCustomerCart(userId);
        return response(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderSummary> list(UUID userId, com.perfumestore.backend.shared.api.PageQuery page) {
        SortOrder requested = SortParser.parse(page.getSort(), SORTS, "createdAt");
        List<OrderRecord> result = orders.findPage(userId, page.getPage(), page.getSize(), requested.property(),
                requested.direction() == SortOrder.Direction.DESC ? "desc" : "asc");
        long total = orders.count(userId);
        return new PageResponse<>(result.stream().map(o -> new OrderDtos.OrderSummary(o.publicNumber(), o.createdAt(), o.status(),
                o.paymentStatus(), o.deliveryStatus(), money(o.total()), orders.items(o.id()).size())).toList(), page.getPage(),
                page.getSize(), total, PageResponse.totalPages(total, page.getSize()),
                SortParser.withTieBreaker(requested, "id"));
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse get(UUID userId, String publicNumber) {
        return response(orders.findByPublicNumberForUser(publicNumber, userId).orElseThrow(() ->
                new ApiRequestException(404, "ORDER_NOT_FOUND", "O pedido não foi encontrado.")));
    }

    @Transactional
    public PaymentCheckoutOrder requirePaymentOrder(UUID userId, String publicNumber) {
        OrderRecord order = orders.findByPublicNumberForUser(publicNumber, userId)
                .flatMap(found -> orders.findByIdForUpdate(found.id()))
                .orElseThrow(() -> new ApiRequestException(404, "ORDER_NOT_FOUND", "O pedido não foi encontrado."));
        return new PaymentCheckoutOrder(order.id(), order.publicNumber(), order.status(), order.paymentStatus(),
                order.total(), order.currency(), order.paymentExpiresAt());
    }

    @Transactional
    public OrderDtos.OrderResponse applyPaymentApproved(UUID orderId, String correlationId) {
        OrderRecord order = lockOrder(orderId);
        if (order.status() == OrderStatus.CONFIRMED && order.paymentStatus() == PaymentStatus.APPROVED) return response(order);
        if (order.status() == OrderStatus.EXPIRED) {
            Instant now = Instant.now();
            orders.updateState(order.id(), OrderStatus.PAYMENT_REVIEW, PaymentStatus.APPROVED, now);
            orders.insertHistory(UUID.randomUUID(), order.id(), order.status().name(), OrderStatus.PAYMENT_REVIEW.name(),
                    "LATE_PAYMENT_APPROVED", null, correlationId, now);
            return response(orderWith(order, OrderStatus.PAYMENT_REVIEW, PaymentStatus.APPROVED, now));
        }
        if (order.status() == OrderStatus.PAYMENT_REVIEW) return response(order);
        if (order.status() != OrderStatus.PENDING_PAYMENT) throw conflict("ORDER_STATE_CONFLICT", "O pedido não pode receber esse evento.");
        for (OrderReservationRecord reservation : orders.reservations(order.id())) inventory.confirm(reservation.reservationKey(), null, correlationId);
        Instant now = Instant.now();
        orders.updateState(order.id(), OrderStatus.CONFIRMED, PaymentStatus.APPROVED, now);
        orders.insertHistory(UUID.randomUUID(), order.id(), order.status().name(), OrderStatus.CONFIRMED.name(),
                "PAYMENT_APPROVED", null, correlationId, now);
        return response(orderWith(order, OrderStatus.CONFIRMED, PaymentStatus.APPROVED, now));
    }

    @Transactional
    public OrderDtos.OrderResponse applyPaymentRejected(UUID orderId, PaymentStatus payment, String event,
            String correlationId) {
        OrderRecord order = lockOrder(orderId);
        if (order.status() == OrderStatus.CANCELLED && order.paymentStatus() == payment) return response(order);
        if (order.status() == OrderStatus.EXPIRED || order.status() == OrderStatus.PAYMENT_REVIEW) return response(order);
        if (order.status() != OrderStatus.PENDING_PAYMENT) throw conflict("ORDER_STATE_CONFLICT", "O pedido não pode receber esse evento.");
        for (OrderReservationRecord reservation : orders.reservations(order.id())) inventory.release(reservation.reservationKey(), null, correlationId);
        Instant now = Instant.now();
        orders.updateState(order.id(), OrderStatus.CANCELLED, payment, now);
        orders.insertHistory(UUID.randomUUID(), order.id(), order.status().name(), OrderStatus.CANCELLED.name(), event,
                null, correlationId, now);
        return response(orderWith(order, OrderStatus.CANCELLED, payment, now));
    }

    @Transactional
    public OrderDtos.OrderResponse expireFromPayment(UUID orderId, String correlationId) {
        OrderRecord order = lockOrder(orderId);
        if (order.status() != OrderStatus.PENDING_PAYMENT) return response(order);
        for (OrderReservationRecord reservation : orders.reservations(order.id())) inventory.release(reservation.reservationKey(), null, correlationId);
        Instant now = Instant.now();
        orders.updateState(order.id(), OrderStatus.EXPIRED, PaymentStatus.EXPIRED, now);
        orders.insertHistory(UUID.randomUUID(), order.id(), order.status().name(), OrderStatus.EXPIRED.name(),
                "PAYMENT_DEADLINE_EXPIRED", null, correlationId, now);
        return response(orderWith(order, OrderStatus.EXPIRED, PaymentStatus.EXPIRED, now));
    }

    @Transactional
    public int expireDue(String correlationId) {
        if (!properties.isExpirationEnabled()) return 0;
        properties.validate();
        int count = 0;
        Instant now = Instant.now();
        for (OrderRecord order : orders.findDueForUpdate(now, properties.getExpirationBatchSize())) {
            for (OrderReservationRecord reservation : orders.reservations(order.id())) inventory.release(reservation.reservationKey(), null, correlationId);
            orders.updateState(order.id(), OrderStatus.EXPIRED, PaymentStatus.EXPIRED, now);
            orders.insertHistory(UUID.randomUUID(), order.id(), order.status().name(), OrderStatus.EXPIRED.name(), "PAYMENT_DEADLINE_EXPIRED", null, correlationId, now);
            count++;
        }
        return count;
    }

    private OrderDtos.OrderResponse existingOrConflict(OrderRecord previous, String fingerprint) {
        if (!previous.requestFingerprint().equals(fingerprint)) throw conflict("IDEMPOTENCY_KEY_REUSED", "A chave de idempotência foi usada com dados diferentes.");
        return response(previous);
    }
    private OrderDtos.OrderResponse response(OrderRecord order) {
        List<OrderDtos.OrderItemResponse> items = orders.items(order.id()).stream().map(i -> new OrderDtos.OrderItemResponse(i.presentationId(), i.productId(), i.sku(), i.productName(), i.volume(), i.unit(), i.quantity(), money(i.unitPrice()), money(i.lineTotal()))).toList();
        OrderAddressRecord a = orders.address(order.id());
        OrderShippingRecord s = orders.shipping(order.id());
        List<OrderDtos.HistoryResponse> history = orders.history(order.id()).stream().map(h -> new OrderDtos.HistoryResponse(h.fromStatus(), h.toStatus(), h.event(), h.createdAt())).toList();
        return new OrderDtos.OrderResponse(order.publicNumber(), order.createdAt(), order.updatedAt(), order.status(), order.paymentStatus(), order.deliveryStatus(), money(order.subtotal()), money(order.shippingAmount()), money(order.total()), order.paymentExpiresAt(), items, new OrderDtos.AddressSnapshot(a.recipientName(), a.label(), a.postalCode(), a.street(), a.number(), a.complement(), a.neighborhood(), a.city(), a.state(), a.reference()), new OrderDtos.ShippingSnapshot(s.quoteId(), s.method(), money(s.amount()), s.minDays(), s.maxDays(), s.dayType(), s.ruleVersion()), history);
    }
    private OrderRecord lockOrder(UUID orderId) {
        return orders.findByIdForUpdate(orderId).orElseThrow(() ->
                new ApiRequestException(404, "ORDER_NOT_FOUND", "O pedido não foi encontrado."));
    }
    private OrderRecord orderWith(OrderRecord o, OrderStatus status, PaymentStatus payment, Instant now) { return new OrderRecord(o.id(), o.publicNumber(), o.userId(), o.cartId(), o.idempotencyKey(), o.requestFingerprint(), status, payment, o.deliveryStatus(), o.subtotal(), o.shippingAmount(), o.total(), o.currency(), o.createdAt(), now, o.paymentExpiresAt()); }
    private MoneyResponse money(BigDecimal value) { return new MoneyResponse(value.setScale(2), "BRL"); }
    private String publicNumber() { return "PS-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(); }
    private String reservationKey(UUID orderId, UUID presentationId) { return "o:" + orderId.toString().replace("-", "") + ":" + presentationId.toString().replace("-", ""); }
    private String digest(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder b = new StringBuilder(64); for (byte v : bytes) b.append(String.format("%02x", v)); return b.toString(); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
    private void validateKey(String key) { if (key == null || !key.matches("[A-Za-z0-9._:-]{8,100}")) throw new ApiRequestException(422, "IDEMPOTENCY_KEY_INVALID", "Informe uma chave de idempotência válida."); }
    private ApiRequestException conflict(String code, String message) { return new ApiRequestException(409, code, message); }
    private record CartLine(UUID presentationId, int quantity, CatalogService.CartPresentation presentation, BigDecimal lineTotal) { }
    public record PaymentCheckoutOrder(UUID id, String publicNumber, OrderStatus status, PaymentStatus paymentStatus,
            BigDecimal total, String currency, Instant paymentExpiresAt) { }
}
