package com.perfumestore.backend.order;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.perfumestore.backend.auth.AppUserPrincipal;
import com.perfumestore.backend.inventory.InventoryService;
import com.perfumestore.backend.notification.NotificationService;
import com.perfumestore.backend.payment.RefundDtos;
import com.perfumestore.backend.payment.RefundService;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.shared.api.SortParser;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderService {
    private static final Map<String, String> SORTS = Map.of("id", "id", "createdAt", "created_at", "total", "total",
            "status", "order_status", "paymentStatus", "payment_status", "deliveryStatus", "delivery_status");
    private final OrderRepository orders;
    private final AdminOrderRepository admin;
    private final UserRepository users;
    private final OrderService orderService;
    private final InventoryService inventory;
    private final RefundService refunds;
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;

    public AdminOrderService(OrderRepository orders, AdminOrderRepository admin, UserRepository users,
            OrderService orderService, InventoryService inventory, RefundService refunds, JdbcTemplate jdbc,
            NotificationService notifications) {
        this.orders = orders; this.admin = admin; this.users = users; this.orderService = orderService;
        this.inventory = inventory; this.refunds = refunds; this.jdbc = jdbc;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderDtos.Summary> list(PageQuery page, String search, OrderStatus status,
            PaymentStatus paymentStatus, DeliveryStatus deliveryStatus, Instant from, Instant to) {
        SortOrder requested = SortParser.parse(page.getSort(), SORTS, "createdAt");
        List<OrderRecord> result = orders.findAdminPage(page.getPage(), page.getSize(), requested.property(),
                requested.direction() == SortOrder.Direction.DESC ? "desc" : "asc", clean(search), status,
                paymentStatus, deliveryStatus, from, to);
        long total = orders.countAdmin(clean(search), status, paymentStatus, deliveryStatus, from, to);
        return new PageResponse<>(result.stream().map(this::summary).toList(), page.getPage(), page.getSize(), total,
                PageResponse.totalPages(total, page.getSize()), SortParser.withTieBreaker(requested, "id"));
    }

    @Transactional(readOnly = true)
    public AdminOrderDtos.Detail detail(String publicNumber) {
        OrderRecord order = require(publicNumber);
        AppUser user = users.findById(order.userId()).orElseThrow();
        List<RefundDtos.Summary> refundSummaries = refunds.summariesForOrder(order.id());
        return detail(order, user, refundSummaries);
    }

    @Transactional
    public AdminOrderDtos.Detail prepare(String publicNumber, String key, UUID actor, String reason, String correlation) {
        String operationKey = operationKey("prepare", publicNumber, key, reason);
        if (admin.findOperation(operationKey).isPresent()) return detail(publicNumber);
        OrderRecord order = lock(publicNumber);
        if (order.deliveryStatus() == DeliveryStatus.PREPARING) return detail(order.publicNumber());
        requireConfirmed(order);
        if (order.deliveryStatus() != DeliveryStatus.NOT_STARTED) throw conflict("ORDER_STATE_CONFLICT", "O pedido nÃ£o estÃ¡ pronto para separaÃ§Ã£o.");
        Instant now = Instant.now();
        orders.updateDeliveryState(order.id(), DeliveryStatus.PREPARING, now);
        admin.insertHistory(UUID.randomUUID(), order.id(), "PREPARE", operationKey, actor, order.status().name(),
                order.status().name(), order.deliveryStatus().name(), DeliveryStatus.PREPARING.name(), clean(reason), null, correlation, now);
        return detail(order.publicNumber());
    }

    @Transactional
    public AdminOrderDtos.Detail ship(String publicNumber, String key, UUID actor, AdminOrderDtos.ShipRequest request,
            String correlation) {
        String operationKey = operationKey("ship", publicNumber, key, request.carrier() + "|" + request.trackingCode() + "|" + request.reason());
        if (admin.findOperation(operationKey).isPresent()) return detail(publicNumber);
        OrderRecord order = lock(publicNumber);
        if (order.deliveryStatus() == DeliveryStatus.SHIPPED || order.deliveryStatus() == DeliveryStatus.DELIVERED) {
            AdminOrderDtos.Logistics existing = admin.logistics(order.id()).orElse(null);
            if (existing != null && same(existing, request)) return detail(publicNumber);
            throw conflict("SHIPMENT_ALREADY_REGISTERED", "O envio jÃ¡ foi registrado com outros dados.");
        }
        requireConfirmed(order);
        if (order.deliveryStatus() != DeliveryStatus.PREPARING) throw conflict("ORDER_NOT_PREPARED", "O pedido ainda nÃ£o foi separado.");
        Instant now = Instant.now();
        admin.insertShipping(order.id(), cleanRequired(request.carrier()), clean(request.trackingCode()), actor, now);
        orders.updateDeliveryState(order.id(), DeliveryStatus.SHIPPED, now);
        admin.insertHistory(UUID.randomUUID(), order.id(), "SHIP", operationKey, actor, order.status().name(),
                order.status().name(), order.deliveryStatus().name(), DeliveryStatus.SHIPPED.name(), clean(request.reason()),
                request.trackingCode(), correlation, now);
        notifications.enqueueOrderShipped(new NotificationService.OrderShippedData(order.id(), order.userId(),
                order.publicNumber(), now, cleanRequired(request.carrier()), clean(request.trackingCode())));
        return detail(order.publicNumber());
    }

    @Transactional
    public AdminOrderDtos.Detail deliver(String publicNumber, String key, UUID actor, String reason, String correlation) {
        String operationKey = operationKey("deliver", publicNumber, key, reason);
        if (admin.findOperation(operationKey).isPresent()) return detail(publicNumber);
        OrderRecord order = lock(publicNumber);
        if (order.deliveryStatus() == DeliveryStatus.DELIVERED) return detail(publicNumber);
        requireConfirmed(order);
        if (order.deliveryStatus() != DeliveryStatus.SHIPPED) throw conflict("ORDER_NOT_SHIPPED", "O pedido ainda nÃ£o foi enviado.");
        Instant now = Instant.now();
        admin.markDelivered(order.id(), actor, now);
        orders.updateDeliveryState(order.id(), DeliveryStatus.DELIVERED, now);
        admin.insertHistory(UUID.randomUUID(), order.id(), "DELIVER", operationKey, actor, order.status().name(),
                order.status().name(), order.deliveryStatus().name(), DeliveryStatus.DELIVERED.name(), clean(reason), null, correlation, now);
        return detail(publicNumber);
    }

    @Transactional
    public AdminOrderDtos.Detail cancel(String publicNumber, String key, UUID actor, String reason, String correlation) {
        String operationKey = operationKey("cancel", publicNumber, key, reason);
        if (admin.findOperation(operationKey).isPresent()) return detail(publicNumber);
        OrderRecord order = lock(publicNumber);
        if (order.status() == OrderStatus.CANCELLED) return detail(publicNumber);
        if (order.status() == OrderStatus.PAYMENT_REVIEW || order.deliveryStatus() == DeliveryStatus.SHIPPED
                || order.deliveryStatus() == DeliveryStatus.DELIVERED) {
            throw conflict("RETURN_REQUIRED", "O pedido exige fluxo de devoluÃ§Ã£o e nÃ£o pode ser cancelado diretamente.");
        }
        if (order.status() != OrderStatus.PENDING_PAYMENT
                && !(order.status() == OrderStatus.CONFIRMED && order.paymentStatus() == PaymentStatus.APPROVED
                        && order.deliveryStatus() == DeliveryStatus.NOT_STARTED)) {
            throw conflict("ORDER_STATE_CONFLICT", "O pedido nÃ£o pode ser cancelado neste estado.");
        }
        if (order.status() == OrderStatus.PENDING_PAYMENT) {
            for (OrderReservationRecord reservation : orders.reservations(order.id())) {
                inventory.release(reservation.reservationKey(), actor, correlation);
            }
        }
        Instant now = Instant.now();
        orders.updateState(order.id(), OrderStatus.CANCELLED, order.paymentStatus(), now);
        admin.insertHistory(UUID.randomUUID(), order.id(), "CANCEL", operationKey, actor, order.status().name(),
                OrderStatus.CANCELLED.name(), order.deliveryStatus().name(), order.deliveryStatus().name(), cleanRequired(reason),
                order.paymentStatus() == PaymentStatus.APPROVED ? "REFUND_REQUIRED" : null, correlation, now);
        return detail(publicNumber);
    }

    private AdminOrderDtos.Detail detail(OrderRecord order, AppUser user, List<RefundDtos.Summary> refundSummaries) {
        List<AdminOrderDtos.History> history = admin.history(order.id()).stream().map(h -> new AdminOrderDtos.History(
                h.operation(), h.actorUserId(), h.fromOrderStatus(), h.toOrderStatus(), h.fromDeliveryStatus(),
                h.toDeliveryStatus(), h.reason(), h.externalReference(), h.createdAt())).toList();
        List<ReturnDtos.Summary> returns = jdbc.query("select id, order_id, order_item_id, presentation_id, quantity, received_quantity, approved_quantity, rejected_quantity, status, reason, requested_at, received_at, inspected_at from perfumestore.returns where order_id = ? order by requested_at, id",
                (rs, row) -> new ReturnDtos.Summary(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                        rs.getObject(3, UUID.class), rs.getObject(4, UUID.class), rs.getInt(5), rs.getInt(6), rs.getInt(7),
                        rs.getInt(8), ReturnStatus.valueOf(rs.getString(9)), rs.getString(10), rs.getTimestamp(11).toInstant(),
                        rs.getTimestamp(12) == null ? null : rs.getTimestamp(12).toInstant(),
                        rs.getTimestamp(13) == null ? null : rs.getTimestamp(13).toInstant()), order.id());
        return new AdminOrderDtos.Detail(orderService.adminResponse(order.id()), new AdminOrderDtos.Customer(user.id(), user.displayName(), user.email()),
                admin.logistics(order.id()).orElse(null), history, admin.reservations(order.id()), refundSummaries, returns);
    }

    private AdminOrderDtos.Summary summary(OrderRecord order) {
        return new AdminOrderDtos.Summary(order.publicNumber(), order.userId(), order.createdAt(), order.status(),
                order.paymentStatus(), order.deliveryStatus(), new MoneyResponse(order.total().setScale(2), order.currency()),
                orders.items(order.id()).size());
    }

    private void requireConfirmed(OrderRecord order) {
        if (order.status() != OrderStatus.CONFIRMED || order.paymentStatus() != PaymentStatus.APPROVED
                || order.status() == OrderStatus.PAYMENT_REVIEW) {
            throw conflict("ORDER_NOT_READY", "O pedido nÃ£o estÃ¡ confirmado para esta operaÃ§Ã£o.");
        }
    }

    private boolean same(AdminOrderDtos.Logistics existing, AdminOrderDtos.ShipRequest request) {
        return cleanRequired(request.carrier()).equals(existing.carrier()) && clean(request.trackingCode()).equals(existing.trackingCode());
    }

    private OrderRecord require(String publicNumber) {
        return orders.findByPublicNumber(publicNumber).orElseThrow(() ->
                new ApiRequestException(404, "ORDER_NOT_FOUND", "O pedido nÃ£o foi encontrado."));
    }
    private OrderRecord lock(String publicNumber) { return orders.findByPublicNumber(publicNumber).flatMap(o -> orders.findByIdForUpdate(o.id())).orElseThrow(() ->
            new ApiRequestException(404, "ORDER_NOT_FOUND", "O pedido nÃ£o foi encontrado.")); }
    private String operationKey(String operation, String order, String supplied, String payload) {
        if (supplied != null && !supplied.isBlank()) {
            if (!supplied.matches("[A-Za-z0-9._:-]{8,100}")) throw new ApiRequestException(422, "IDEMPOTENCY_KEY_INVALID", "Informe uma chave de idempotÃªncia vÃ¡lida.");
            return "admin:" + operation + ":" + supplied;
        }
        return "admin:" + operation + ":" + digest(order + "|" + payload);
    }
    private String digest(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String cleanRequired(String value) { if (value == null || value.isBlank()) throw new ApiRequestException(422, "REASON_REQUIRED", "Informe um motivo."); return value.trim(); }
    private ApiRequestException conflict(String code, String message) { return new ApiRequestException(409, code, message); }
}
