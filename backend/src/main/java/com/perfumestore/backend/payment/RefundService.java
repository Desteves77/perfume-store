package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.order.OrderRecord;
import com.perfumestore.backend.order.OrderRepository;
import com.perfumestore.backend.order.OrderStatus;
import com.perfumestore.backend.order.PaymentStatus;
import com.perfumestore.backend.config.PaymentProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {
    private final RefundRepository refunds;
    private final PaymentRepository payments;
    private final PaymentProvider provider;
    private final PaymentProperties properties;
    private final OrderRepository orders;

    public RefundService(RefundRepository refunds, PaymentRepository payments, PaymentProvider provider,
            PaymentProperties properties, OrderRepository orders) {
        this.refunds = refunds; this.payments = payments; this.provider = provider; this.properties = properties; this.orders = orders;
    }

    @Transactional
    public RefundDtos.Detail request(String publicNumber, UUID actor, String reason, String key, String correlation) {
        properties.validate();
        if (!properties.isEnabled()) throw unavailable();
        OrderRecord order = orders.findByPublicNumber(publicNumber).flatMap(o -> orders.findByIdForUpdate(o.id())).orElseThrow(() ->
                new ApiRequestException(404, "ORDER_NOT_FOUND", "O pedido nÃ£o foi encontrado."));
        RefundRecord existing = refunds.findByOrderForUpdate(order.id()).orElse(null);
        String normalizedReason = required(reason);
        String idempotency = normalizeKey(key, "refund:" + publicNumber + ":" + digest(normalizedReason));
        String fingerprint = digest(order.id() + "|" + normalizedReason);
        if (existing != null) {
            if (!existing.requestFingerprint().equals(fingerprint)) throw conflict("REFUND_ALREADY_REQUESTED", "JÃ¡ existe uma solicitaÃ§Ã£o de reembolso para este pedido.");
            return detail(existing);
        }
        if (order.paymentStatus() != PaymentStatus.APPROVED) throw conflict("REFUND_NOT_ELIGIBLE", "O pedido nÃ£o possui pagamento aprovado para reembolso.");
        if (order.status() == OrderStatus.PENDING_PAYMENT || order.status() == OrderStatus.EXPIRED || order.status() == OrderStatus.PAYMENT_REVIEW) {
            throw conflict("REFUND_NOT_ELIGIBLE", "O estado do pedido nÃ£o permite solicitar reembolso.");
        }
        PaymentAttemptRecord payment = payments.findApprovedForOrderForUpdate(order.id()).orElseThrow(() ->
                conflict("REFUND_PAYMENT_NOT_FOUND", "NÃ£o foi encontrada uma tentativa de pagamento aprovada."));
        Instant now = Instant.now();
        RefundRecord shell = new RefundRecord(UUID.randomUUID(), order.id(), payment.id(), order.total(), order.currency(),
                normalizedReason, RefundStatus.REQUESTED, idempotency, fingerprint, null, actor, now, now, null);
        refunds.insert(shell);
        refunds.insertHistory(UUID.randomUUID(), shell.id(), null, RefundStatus.REQUESTED.name(), "REFUND_REQUESTED",
                actor, null, normalizedReason, correlation, now);
        PaymentProvider.ProviderRefundResult result = provider.createRefund(shell.id(), payment.providerPaymentId(), shell.amount(), shell.currency(), now.plus(properties.getCheckoutTtl()));
        refunds.updateProvider(shell.id(), result.providerRefundId(), RefundStatus.PENDING_PROVIDER, Instant.now());
        refunds.insertHistory(UUID.randomUUID(), shell.id(), RefundStatus.REQUESTED.name(), RefundStatus.PENDING_PROVIDER.name(),
                "REFUND_SUBMITTED_TO_PROVIDER", actor, result.providerRefundId(), normalizedReason, correlation, Instant.now());
        return detail(refunds.findByIdForUpdate(shell.id()).orElseThrow());
    }

    @Transactional
    public RefundDtos.Detail reconcile(UUID refundId, UUID actor, String correlation) {
        RefundRecord refund = refunds.findByIdForUpdate(refundId).orElseThrow(() ->
                new ApiRequestException(404, "REFUND_NOT_FOUND", "O reembolso nÃ£o foi encontrado."));
        if (refund.status() == RefundStatus.CONFIRMED || refund.status() == RefundStatus.FAILED) return detail(refund);
        if (refund.providerRef() == null) throw conflict("REFUND_PROVIDER_PENDING", "O reembolso ainda nÃ£o possui referÃªncia do provedor.");
        PaymentProvider.ProviderRefundSnapshot result = provider.getRefundStatus(refund.providerRef());
        if (result.status() == PaymentAttemptStatus.PENDING) return detail(refund);
        Instant now = Instant.now();
        if (result.amount() == null || result.currency() == null || result.amount().compareTo(refund.amount()) != 0
                || !refund.currency().equals(result.currency())) {
            transition(refund, RefundStatus.REVIEW_REQUIRED, "REFUND_VALUE_MISMATCH", actor, correlation, now);
            return detail(refunds.findByIdForUpdate(refund.id()).orElseThrow());
        }
        if (result.status() == PaymentAttemptStatus.APPROVED) {
            transition(refund, RefundStatus.CONFIRMED, "REFUND_PROVIDER_CONFIRMED", actor, correlation, now);
            orders.updatePaymentStatus(refund.orderId(), PaymentStatus.REFUNDED, now);
        } else if (result.status() == PaymentAttemptStatus.FAILED) {
            transition(refund, RefundStatus.FAILED, "REFUND_PROVIDER_FAILED", actor, correlation, now);
        } else {
            transition(refund, RefundStatus.REVIEW_REQUIRED, "REFUND_PROVIDER_UNKNOWN", actor, correlation, now);
        }
        return detail(refunds.findByIdForUpdate(refund.id()).orElseThrow());
    }

    @Transactional
    public RefundDtos.Detail simulate(UUID refundId, String outcome, UUID actor, String correlation) {
        RefundRecord refund = refunds.findByIdForUpdate(refundId).orElseThrow(() ->
                new ApiRequestException(404, "REFUND_NOT_FOUND", "O reembolso nÃ£o foi encontrado."));
        PaymentAttemptStatus status = switch (outcome == null ? "" : outcome) {
            case "APPROVED" -> PaymentAttemptStatus.APPROVED;
            case "FAILED" -> PaymentAttemptStatus.FAILED;
            case "PENDING" -> PaymentAttemptStatus.PENDING;
            case "UNKNOWN", "TIMEOUT" -> PaymentAttemptStatus.UNKNOWN;
            default -> throw new ApiRequestException(422, "REFUND_OUTCOME_INVALID", "Resultado simulado de reembolso invÃ¡lido.");
        };
        if (refund.providerRef() == null) throw conflict("REFUND_PROVIDER_PENDING", "O reembolso ainda nÃ£o foi enviado ao provedor.");
        provider.simulateRefund(refund.providerRef(), status, refund.amount(), refund.currency());
        return reconcile(refund.id(), actor, correlation);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundDtos.Summary> list(PageQuery page, RefundStatus status) {
        String statusValue = status == null ? null : status.name();
        List<RefundRecord> records = refunds.findPage(page, statusValue);
        long total = refunds.count(statusValue);
        return new PageResponse<>(records.stream().map(this::summary).toList(), page.getPage(), page.getSize(), total,
                PageResponse.totalPages(total, page.getSize()), List.of(new SortOrder("requestedAt", SortOrder.Direction.DESC), new SortOrder("id", SortOrder.Direction.DESC)));
    }

    @Transactional(readOnly = true)
    public RefundDtos.Detail get(UUID refundId) {
        return detail(refunds.findByIdForUpdate(refundId).orElseThrow(() -> new ApiRequestException(404, "REFUND_NOT_FOUND", "O reembolso nÃ£o foi encontrado.")));
    }

    @Transactional(readOnly = true)
    public List<RefundDtos.Summary> summariesForOrder(UUID orderId) { return refunds.findByOrder(orderId).stream().map(this::summary).toList(); }

    private void transition(RefundRecord refund, RefundStatus target, String event, UUID actor, String correlation, Instant now) {
        if (refund.status() == target) return;
        if (refund.status() == RefundStatus.CONFIRMED || refund.status() == RefundStatus.FAILED) return;
        refunds.updateStatus(refund.id(), target, now, target == RefundStatus.CONFIRMED || target == RefundStatus.FAILED ? now : null);
        refunds.insertHistory(UUID.randomUUID(), refund.id(), refund.status().name(), target.name(), event, actor,
                refund.providerRef(), null, correlation, now);
    }
    private RefundDtos.Detail detail(RefundRecord refund) { return new RefundDtos.Detail(summary(refund), refunds.history(refund.id())); }
    private RefundDtos.Summary summary(RefundRecord refund) { return new RefundDtos.Summary(refund.id(), refund.orderId(), refund.paymentAttemptId(), new MoneyResponse(refund.amount().setScale(2), refund.currency()), refund.status(), refund.reason(), refund.providerRef(), refund.requestedBy(), refund.requestedAt(), refund.updatedAt(), refund.completedAt()); }
    private String normalizeKey(String key, String fallback) { if (key == null || key.isBlank()) return fallback; if (!key.matches("[A-Za-z0-9._:-]{8,100}")) throw new ApiRequestException(422, "IDEMPOTENCY_KEY_INVALID", "Informe uma chave de idempotÃªncia vÃ¡lida."); return key; }
    private String required(String value) { if (value == null || value.isBlank() || value.trim().length() > 255) throw new ApiRequestException(422, "REFUND_REASON_REQUIRED", "Informe um motivo vÃ¡lido para o reembolso."); return value.trim(); }
    private String digest(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private ApiRequestException conflict(String code, String message) { return new ApiRequestException(409, code, message); }
    private ApiRequestException unavailable() { return new ApiRequestException(503, "REFUND_PROVIDER_UNAVAILABLE", "O provedor de reembolso nÃ£o estÃ¡ disponÃ­vel neste ambiente."); }
}
