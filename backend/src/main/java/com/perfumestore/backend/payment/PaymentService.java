package com.perfumestore.backend.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import com.perfumestore.backend.config.PaymentProperties;
import com.perfumestore.backend.order.OrderDtos;
import com.perfumestore.backend.order.OrderService;
import com.perfumestore.backend.order.PaymentStatus;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.MoneyResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentProvider provider;
    private final PaymentProperties properties;
    private final OrderService orders;

    public PaymentService(PaymentRepository payments, PaymentProvider provider, PaymentProperties properties,
            OrderService orders) {
        this.payments = payments;
        this.provider = provider;
        this.properties = properties;
        this.orders = orders;
    }

    @Transactional
    public PaymentCreateResult createCheckout(UUID userId, String publicNumber, String idempotencyKey,
            String correlationId) {
        properties.validate();
        if (!properties.isEnabled()) throw unavailable();
        validateKey(idempotencyKey);
        OrderService.PaymentCheckoutOrder order = orders.requirePaymentOrder(userId, publicNumber);
        if (order.status().name().equals("EXPIRED") || order.status().name().equals("CANCELLED")
                || order.status().name().equals("CONFIRMED") || order.status().name().equals("PAYMENT_REVIEW")) {
            throw new ApiRequestException(409, "PAYMENT_NOT_ACTIONABLE", "O pedido não pode iniciar um novo pagamento.");
        }
        String fingerprint = digest(order.id().toString());
        PaymentAttemptRecord existing = payments.findByOrderAndKeyForUpdate(order.id(), idempotencyKey).orElse(null);
        if (existing != null) {
            ensureFingerprint(existing, fingerprint);
            return new PaymentCreateResult(response(existing), false);
        }
        PaymentAttemptRecord latest = payments.findLatestForOrderForUpdate(order.id()).orElse(null);
        if (latest != null && (latest.status() == PaymentAttemptStatus.PENDING || latest.status() == PaymentAttemptStatus.UNKNOWN
                || latest.status() == PaymentAttemptStatus.REVIEW)) {
            throw new ApiRequestException(409, "PAYMENT_ATTEMPT_PENDING",
                    "Já existe uma tentativa de pagamento em processamento para este pedido.");
        }
        Instant now = Instant.now();
        Instant expiresAt = order.paymentExpiresAt().isBefore(now.plus(properties.getCheckoutTtl()))
                ? order.paymentExpiresAt() : now.plus(properties.getCheckoutTtl());
        PaymentAttemptRecord shell = new PaymentAttemptRecord(UUID.randomUUID(), order.id(), provider.code(), null, null,
                idempotencyKey, fingerprint, order.total(), order.currency(), PaymentAttemptStatus.PENDING, null, null,
                now, now, null, expiresAt, null);
        if (!payments.insertAttempt(shell)) {
            PaymentAttemptRecord concurrent = payments.findByOrderAndKeyForUpdate(order.id(), idempotencyKey).orElseThrow();
            ensureFingerprint(concurrent, fingerprint);
            return new PaymentCreateResult(response(concurrent), false);
        }
        PaymentProvider.ProviderCheckoutSession session = provider.createCheckout(shell.id(), order.id(), order.total(),
                order.currency(), expiresAt);
        payments.attachProvider(shell.id(), session.providerPaymentId(), session.checkoutSessionId(), Instant.now());
        payments.insertHistory(UUID.randomUUID(), shell.id(), null, PaymentAttemptStatus.PENDING.name(),
                "CHECKOUT_CREATED", correlationId, Instant.now());
        PaymentAttemptRecord created = payments.findByIdForUpdate(shell.id()).orElseThrow();
        return new PaymentCreateResult(response(created, session.checkoutUrl()), true);
    }

    @Transactional
    public PaymentDtos.PaymentStatusResponse current(UUID userId, String publicNumber) {
        OrderService.PaymentCheckoutOrder order = orders.requirePaymentOrder(userId, publicNumber);
        PaymentAttemptRecord attempt = payments.findLatestForOrderForUpdate(order.id()).orElseThrow(() ->
                new ApiRequestException(404, "PAYMENT_NOT_FOUND", "Ainda não existe uma tentativa de pagamento para este pedido."));
        return statusResponse(attempt);
    }

    @Transactional
    public PaymentDtos.WebhookResponse processWebhook(PaymentDtos.EventPayload payload, String payloadHash,
            boolean signatureValid, String correlationId) {
        validatePayload(payload);
        Instant received = Instant.now();
        PaymentEventRecord event = new PaymentEventRecord(UUID.randomUUID(), "MOCK", payload.eventId(),
                payload.eventType(), payload.providerPaymentId(), payload.occurredAt(), received, payload.amount(),
                payload.currency(), payloadHash, signatureValid, PaymentEventProcessingStatus.RECEIVED, null, null);
        if (!payments.insertEvent(event)) return duplicateResponse(payload);
        PaymentAttemptRecord attempt = payments.findByProviderPaymentIdForUpdate("MOCK", payload.providerPaymentId()).orElse(null);
        if (attempt == null) return rejectEvent(payload, "PAYMENT_NOT_FOUND", null);
        if (payload.amount() == null || payload.currency() == null || payload.amount().compareTo(attempt.amount()) != 0
                || !attempt.currency().equals(payload.currency())) {
            payments.updateStatus(attempt.id(), PaymentAttemptStatus.REVIEW, "PAYMENT_AMOUNT_MISMATCH",
                    "O evento não corresponde ao valor ou moeda da tentativa.", received, null);
            payments.insertHistory(UUID.randomUUID(), attempt.id(), attempt.status().name(), PaymentAttemptStatus.REVIEW.name(),
                    "PAYMENT_VALUE_MISMATCH", correlationId, received);
            payments.updateEvent("MOCK", payload.eventId(), PaymentEventProcessingStatus.REVIEW,
                    "PAYMENT_AMOUNT_MISMATCH", received);
            return new PaymentDtos.WebhookResponse(payload.eventId(), attempt.id().toString(), "REVIEW", "REVIEW");
        }
        PaymentEventType type;
        try { type = PaymentEventType.valueOf(payload.eventType()); }
        catch (IllegalArgumentException exception) { return rejectEvent(payload, "PAYMENT_EVENT_INVALID", attempt); }
        PaymentAttemptStatus target = targetStatus(type);
        payments.updateMockStateIfNewer(attempt.providerPaymentId(), target == PaymentAttemptStatus.UNKNOWN
                ? PaymentAttemptStatus.PENDING : target, attempt.amount(), attempt.currency(), payload.eventId(),
                payload.occurredAt(), received);
        if (isObsolete(attempt.status(), target)) {
            payments.updateEvent("MOCK", payload.eventId(), PaymentEventProcessingStatus.IGNORED, null, received);
            return new PaymentDtos.WebhookResponse(payload.eventId(), attempt.id().toString(), "IGNORED", attempt.status().name());
        }
        if (type == PaymentEventType.PAYMENT_PENDING) {
            payments.updateEvent("MOCK", payload.eventId(), PaymentEventProcessingStatus.PROCESSED, null, received);
            return new PaymentDtos.WebhookResponse(payload.eventId(), attempt.id().toString(), "PROCESSED", attempt.status().name());
        }
        payments.updateStatus(attempt.id(), target, null, null, received,
                target == PaymentAttemptStatus.PENDING ? null : received);
        payments.insertHistory(UUID.randomUUID(), attempt.id(), attempt.status().name(), target.name(), type.name(),
                correlationId, received);
        if (type == PaymentEventType.PAYMENT_APPROVED) {
            orders.applyPaymentApproved(attempt.orderId(), correlationId);
        } else if (type == PaymentEventType.PAYMENT_EXPIRED) {
            orders.expireFromPayment(attempt.orderId(), correlationId);
        } else if (type == PaymentEventType.PAYMENT_REJECTED || type == PaymentEventType.PAYMENT_FAILED
                || type == PaymentEventType.PAYMENT_CANCELLED) {
            PaymentStatus orderPayment = type == PaymentEventType.PAYMENT_FAILED ? PaymentStatus.FAILED : PaymentStatus.REJECTED;
            orders.applyPaymentRejected(attempt.orderId(), orderPayment, type.name(), correlationId);
        }
        payments.updateEvent("MOCK", payload.eventId(), PaymentEventProcessingStatus.PROCESSED, null, Instant.now());
        return new PaymentDtos.WebhookResponse(payload.eventId(), attempt.id().toString(), "PROCESSED", target.name());
    }

    @Transactional
    public OrderDtos.OrderResponse simulate(UUID userId, String publicNumber, String outcome, String correlationId) {
        if (outcome == null) throw new ApiRequestException(422, "PAYMENT_OUTCOME_REQUIRED", "Informe o resultado simulado.");
        OrderService.PaymentCheckoutOrder order = orders.requirePaymentOrder(userId, publicNumber);
        PaymentAttemptRecord existing = payments.findLatestForOrderForUpdate(order.id()).orElse(null);
        PaymentAttemptRecord attempt;
        if (existing == null) {
            createCheckout(userId, publicNumber,
                    "dev-payment-" + order.id().toString().replace("-", "").substring(0, 20), correlationId);
            attempt = payments.findByOrderAndKeyForUpdate(order.id(),
                    "dev-payment-" + order.id().toString().replace("-", "").substring(0, 20)).orElseThrow();
        } else {
            attempt = existing;
        }
        PaymentEventType eventType = switch (outcome) {
            case "APPROVED" -> PaymentEventType.PAYMENT_APPROVED;
            case "REJECTED" -> PaymentEventType.PAYMENT_REJECTED;
            case "FAILED" -> PaymentEventType.PAYMENT_FAILED;
            default -> throw new ApiRequestException(422, "PAYMENT_OUTCOME_INVALID", "Resultado simulado inválido.");
        };
        PaymentDtos.EventPayload payload = new PaymentDtos.EventPayload(
                "dev-" + attempt.id().toString().replace("-", "") + "-" + eventType.name(), eventType.name(),
                attempt.providerPaymentId(), Instant.now(), attempt.amount(), attempt.currency());
        processWebhook(payload, digest(payload.toString()), true, correlationId);
        return orders.get(userId, publicNumber);
    }

    @Transactional
    public PaymentDtos.ReconciliationResponse reconcile(UUID paymentId, String correlationId) {
        properties.validate();
        PaymentAttemptRecord attempt = payments.findByIdForUpdate(paymentId).orElseThrow(() ->
                new ApiRequestException(404, "PAYMENT_NOT_FOUND", "A tentativa de pagamento não foi encontrada."));
        Instant now = Instant.now();
        PaymentProvider.ProviderPaymentSnapshot snapshot = provider.getPaymentStatus(attempt.providerPaymentId());
        if (snapshot.status() == PaymentAttemptStatus.UNKNOWN || snapshot.status() == PaymentAttemptStatus.PENDING) {
            payments.updateProviderCheck(attempt.id(), now);
            payments.insertReconciliation(UUID.randomUUID(), attempt.id(), provider.code(), snapshot.status().name(),
                    "NO_CHANGE", null, now, now);
            return new PaymentDtos.ReconciliationResponse(attempt.id(), "NO_CHANGE", snapshot.status().name(), attempt.status().name());
        }
        if (snapshot.amount() == null || snapshot.currency() == null || snapshot.amount().compareTo(attempt.amount()) != 0
                || !attempt.currency().equals(snapshot.currency())) {
            payments.updateStatus(attempt.id(), PaymentAttemptStatus.REVIEW, "PAYMENT_AMOUNT_MISMATCH",
                    "A reconciliação retornou valor ou moeda divergente.", now, null);
            payments.insertReconciliation(UUID.randomUUID(), attempt.id(), provider.code(), snapshot.status().name(),
                    "REVIEW", "PAYMENT_AMOUNT_MISMATCH", now, now);
            return new PaymentDtos.ReconciliationResponse(attempt.id(), "REVIEW", snapshot.status().name(), "REVIEW");
        }
        PaymentEventType eventType = eventFor(snapshot.status());
        PaymentDtos.EventPayload payload = new PaymentDtos.EventPayload(
                "reconcile-" + attempt.id().toString().replace("-", "") + "-" + snapshot.status().name() + "-" + snapshot.occurredAt().toEpochMilli(),
                eventType.name(), attempt.providerPaymentId(), snapshot.occurredAt(), attempt.amount(), attempt.currency());
        PaymentDtos.WebhookResponse processed = processWebhook(payload, digest(payload.toString()), true, correlationId);
        payments.updateProviderCheck(attempt.id(), now);
        payments.insertReconciliation(UUID.randomUUID(), attempt.id(), provider.code(), snapshot.status().name(),
                "APPLIED", null, now, now);
        return new PaymentDtos.ReconciliationResponse(attempt.id(), "APPLIED", snapshot.status().name(), processed.paymentStatus());
    }

    private PaymentDtos.WebhookResponse duplicateResponse(PaymentDtos.EventPayload payload) {
        PaymentEventRecord event = payments.findEvent("MOCK", payload.eventId()).orElseThrow();
        PaymentAttemptRecord attempt = payments.findByProviderPaymentIdForUpdate("MOCK", payload.providerPaymentId()).orElse(null);
        return new PaymentDtos.WebhookResponse(payload.eventId(), attempt == null ? null : attempt.id().toString(),
                event.processingStatus().name(), attempt == null ? null : attempt.status().name());
    }

    private PaymentDtos.WebhookResponse rejectEvent(PaymentDtos.EventPayload payload, String code, PaymentAttemptRecord attempt) {
        payments.updateEvent("MOCK", payload.eventId(), PaymentEventProcessingStatus.REJECTED, code, Instant.now());
        return new PaymentDtos.WebhookResponse(payload.eventId(), attempt == null ? null : attempt.id().toString(), "REJECTED",
                attempt == null ? null : attempt.status().name());
    }

    private boolean isObsolete(PaymentAttemptStatus current, PaymentAttemptStatus incoming) {
        if (current == PaymentAttemptStatus.APPROVED) return true;
        if (current == PaymentAttemptStatus.REJECTED || current == PaymentAttemptStatus.FAILED
                || current == PaymentAttemptStatus.CANCELLED || current == PaymentAttemptStatus.EXPIRED) {
            return incoming != PaymentAttemptStatus.APPROVED;
        }
        return false;
    }

    private PaymentAttemptStatus targetStatus(PaymentEventType type) {
        return switch (type) {
            case PAYMENT_PENDING -> PaymentAttemptStatus.PENDING;
            case PAYMENT_APPROVED -> PaymentAttemptStatus.APPROVED;
            case PAYMENT_REJECTED -> PaymentAttemptStatus.REJECTED;
            case PAYMENT_FAILED -> PaymentAttemptStatus.FAILED;
            case PAYMENT_CANCELLED -> PaymentAttemptStatus.CANCELLED;
            case PAYMENT_EXPIRED -> PaymentAttemptStatus.EXPIRED;
        };
    }

    private PaymentEventType eventFor(PaymentAttemptStatus status) {
        return switch (status) {
            case APPROVED -> PaymentEventType.PAYMENT_APPROVED;
            case REJECTED -> PaymentEventType.PAYMENT_REJECTED;
            case FAILED -> PaymentEventType.PAYMENT_FAILED;
            case CANCELLED -> PaymentEventType.PAYMENT_CANCELLED;
            case EXPIRED -> PaymentEventType.PAYMENT_EXPIRED;
            default -> PaymentEventType.PAYMENT_PENDING;
        };
    }

    private void validatePayload(PaymentDtos.EventPayload payload) {
        if (payload == null || blank(payload.eventId()) || payload.eventId().length() > 120 || blank(payload.eventType())
                || blank(payload.providerPaymentId()) || payload.providerPaymentId().length() > 120 || payload.occurredAt() == null) {
            throw new ApiRequestException(400, "PAYMENT_EVENT_INVALID", "O evento de pagamento é inválido.");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private PaymentDtos.PaymentResponse response(PaymentAttemptRecord attempt) { return response(attempt, null); }
    private PaymentDtos.PaymentResponse response(PaymentAttemptRecord attempt, String checkoutUrl) {
        return new PaymentDtos.PaymentResponse(attempt.id(), attempt.provider(), attempt.providerPaymentId(),
                attempt.status().name(), money(attempt.amount()), checkoutUrl, attempt.createdAt(), attempt.updatedAt(), attempt.expiresAt());
    }
    private PaymentDtos.PaymentStatusResponse statusResponse(PaymentAttemptRecord attempt) {
        return new PaymentDtos.PaymentStatusResponse(attempt.id(), attempt.provider(), attempt.providerPaymentId(),
                attempt.status().name(), money(attempt.amount()), attempt.createdAt(), attempt.updatedAt(), attempt.expiresAt());
    }
    private MoneyResponse money(BigDecimal amount) { return new MoneyResponse(amount.setScale(2), "BRL"); }
    private void ensureFingerprint(PaymentAttemptRecord attempt, String expected) {
        if (!attempt.requestFingerprint().equals(expected)) throw new ApiRequestException(409, "IDEMPOTENCY_KEY_REUSED",
                "A chave de idempotência foi usada com dados diferentes.");
    }
    private void validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,100}")) throw new ApiRequestException(422,
                "IDEMPOTENCY_KEY_INVALID", "Informe uma chave de idempotência válida.");
    }
    private ApiRequestException unavailable() { return new ApiRequestException(503, "PAYMENT_PROVIDER_UNAVAILABLE",
            "O provedor de pagamentos não está disponível neste ambiente."); }
    private String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    public record PaymentCreateResult(PaymentDtos.PaymentResponse response, boolean created) { }
}
