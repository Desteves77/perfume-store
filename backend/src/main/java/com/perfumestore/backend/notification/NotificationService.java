package com.perfumestore.backend.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.perfumestore.backend.config.NotificationProperties;
import com.perfumestore.backend.order.OrderStatus;
import com.perfumestore.backend.order.PaymentStatus;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.shared.api.SortParser;
import com.perfumestore.backend.user.AppUser;
import com.perfumestore.backend.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final java.util.Map<String, String> SORTS = java.util.Map.of(
            "createdAt", "created_at", "status", "status", "attempts", "attempt_count");

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final NotificationProperties properties;

    public NotificationService(NotificationRepository notifications, UserRepository users,
            NotificationProperties properties) {
        this.notifications = notifications;
        this.users = users;
        this.properties = properties;
    }

    @Transactional
    public void enqueueOrderReceived(OrderCreatedData data) {
        if (!properties.isEnabled()) return;
        AppUser user = users.findById(data.userId()).orElseThrow();
        String subject = "Pedido recebido - " + data.publicNumber();
        StringBuilder body = new StringBuilder();
        body.append("Ola ").append(user.displayName()).append(",\n\n");
        body.append("Recebemos o seu pedido ").append(data.publicNumber()).append(" em ")
                .append(DATE_TIME.format(data.createdAt())).append(" UTC.\n\n");
        body.append("Itens:\n");
        for (Item item : data.items()) {
            body.append("- ").append(item.name()).append(" x ").append(item.quantity())
                    .append(" - R$ ").append(money(item.lineTotal())).append("\n");
        }
        body.append("\nSubtotal: R$ ").append(money(data.subtotal()))
                .append("\nFrete: R$ ").append(money(data.shipping()))
                .append("\nTotal: R$ ").append(money(data.total())).append(" ").append(data.currency())
                .append("\nPagamento: ").append(data.paymentStatus().name()).append("\n");
        StringBuilder html = new StringBuilder("<p>Ola ").append(escape(user.displayName()))
                .append(",</p><p>Recebemos o seu pedido <strong>").append(escape(data.publicNumber()))
                .append("</strong> em ").append(DATE_TIME.format(data.createdAt())).append(" UTC.</p><ul>");
        for (Item item : data.items()) {
            html.append("<li>").append(escape(item.name())).append(" x ").append(item.quantity())
                    .append(" - R$ ").append(money(item.lineTotal())).append("</li>");
        }
        html.append("</ul><p>Subtotal: R$ ").append(money(data.subtotal())).append("<br>Frete: R$ ")
                .append(money(data.shipping())).append("<br>Total: R$ ").append(money(data.total())).append(" ")
                .append(escape(data.currency())).append("</p><p>Pagamento: ")
                .append(escape(data.paymentStatus().name())).append("</p>");
        insert(data.orderId(), data.userId(), user.email(), NotificationType.ORDER_RECEIVED,
                "order:" + data.orderId() + ":received", "order-received", subject, body.toString(), html.toString(), data.createdAt());
    }

    @Transactional
    public void enqueuePaymentApproved(PaymentApprovedData data) {
        if (!properties.isEnabled() || data.orderStatus() != OrderStatus.CONFIRMED
                || data.paymentStatus() != PaymentStatus.APPROVED) return;
        AppUser user = users.findById(data.userId()).orElseThrow();
        String subject = "Pagamento aprovado - " + data.publicNumber();
        String body = "Ola " + user.displayName() + ",\n\n"
                + "O pagamento do pedido " + data.publicNumber() + " foi aprovado.\n"
                + "Valor: R$ " + money(data.total()) + " " + data.currency() + ".\n\n"
                + "O pedido seguirá para as proximas etapas de preparacao.\n";
        String html = "<p>Ola " + escape(user.displayName()) + ",</p><p>O pagamento do pedido <strong>"
                + escape(data.publicNumber()) + "</strong> foi aprovado.</p><p>Valor: R$ " + money(data.total())
                + " " + escape(data.currency()) + ".</p><p>O pedido seguirá para as proximas etapas de preparacao.</p>";
        insert(data.orderId(), data.userId(), user.email(), NotificationType.PAYMENT_APPROVED,
                "order:" + data.orderId() + ":payment-approved", "payment-approved", subject, body, html, data.occurredAt());
    }

    @Transactional
    public void enqueueOrderShipped(OrderShippedData data) {
        if (!properties.isEnabled()) return;
        AppUser user = users.findById(data.userId()).orElseThrow();
        String subject = "Pedido enviado - " + data.publicNumber();
        String tracking = data.trackingCode() == null ? "" : "\nCodigo de rastreamento: " + data.trackingCode();
        String body = "Ola " + user.displayName() + ",\n\n"
                + "O pedido " + data.publicNumber() + " foi enviado em "
                + DATE_TIME.format(data.shippedAt()) + " UTC.\n"
                + "Responsavel pela entrega: " + data.carrier() + "." + tracking + "\n\n"
                + "A entrega ainda nao foi confirmada.\n";
        String html = "<p>Ola " + escape(user.displayName()) + ",</p><p>O pedido <strong>"
                + escape(data.publicNumber()) + "</strong> foi enviado em " + DATE_TIME.format(data.shippedAt())
                + " UTC.</p><p>Responsavel pela entrega: " + escape(data.carrier()) + "."
                + (data.trackingCode() == null ? "" : "<br>Codigo de rastreamento: " + escape(data.trackingCode()))
                + "</p><p>A entrega ainda nao foi confirmada.</p>";
        insert(data.orderId(), data.userId(), user.email(), NotificationType.ORDER_SHIPPED,
                "order:" + data.orderId() + ":shipped", "order-shipped", subject, body, html, data.shippedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDtos.Summary> page(PageQuery page, String status, String eventType) {
        SortOrder requested = SortParser.parse(page.getSort(), SORTS, "createdAt");
        return notifications.page(page.getPage(), page.getSize(), requested.property(),
                requested.direction() == SortOrder.Direction.DESC ? "desc" : "asc",
                status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT),
                eventType == null || eventType.isBlank() ? null : eventType.trim().toUpperCase(Locale.ROOT));
    }

    private void insert(UUID orderId, UUID userId, String recipient, NotificationType type, String eventKey,
            String template, String subject, String body, String html, Instant createdAt) {
        Instant now = Instant.now();
        notifications.insertIfAbsent(new NotificationRecord(UUID.randomUUID(), type, eventKey, orderId, userId,
                recipient, template, "v1", subject, body, html, NotificationStatus.PENDING, 0, now, createdAt, now,
                null, null, null, null));
    }

    private String money(BigDecimal amount) { return amount.setScale(2).toPlainString().replace('.', ','); }
    private String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    public record Item(String name, int quantity, BigDecimal lineTotal) { }
    public record OrderCreatedData(UUID orderId, UUID userId, String publicNumber, Instant createdAt,
            List<Item> items, BigDecimal subtotal, BigDecimal shipping, BigDecimal total, String currency,
            PaymentStatus paymentStatus) { }
    public record PaymentApprovedData(UUID orderId, UUID userId, String publicNumber, Instant occurredAt,
            BigDecimal total, String currency, OrderStatus orderStatus, PaymentStatus paymentStatus) { }
    public record OrderShippedData(UUID orderId, UUID userId, String publicNumber, Instant shippedAt,
            String carrier, String trackingCode) { }
}
