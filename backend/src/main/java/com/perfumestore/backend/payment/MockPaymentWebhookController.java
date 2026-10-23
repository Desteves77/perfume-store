package com.perfumestore.backend.payment;

import com.perfumestore.backend.shared.api.ApiRequestException;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "test"})
@RestController
public class MockPaymentWebhookController {
    private final PaymentService payments;
    private final MockWebhookVerifier verifier;
    private final JsonMapper json;

    public MockPaymentWebhookController(PaymentService payments, MockWebhookVerifier verifier, JsonMapper json) {
        this.payments = payments;
        this.verifier = verifier;
        this.json = json;
    }

    @PostMapping(path = "/api/v1/webhooks/payments/mock", consumes = "application/json")
    public ResponseEntity<PaymentDtos.WebhookResponse> webhook(@RequestBody String body,
            @RequestHeader("X-Mock-Signature") String signature,
            @RequestHeader("X-Mock-Timestamp") String timestamp,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        verifier.verify(body, signature, timestamp);
        PaymentDtos.EventPayload payload;
        try {
            payload = json.readValue(body, PaymentDtos.EventPayload.class);
        } catch (Exception exception) {
            throw new ApiRequestException(400, "PAYMENT_EVENT_INVALID", "O evento de pagamento é inválido.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(payments.processWebhook(payload, verifier.payloadHash(body), true, correlation));
    }
}
