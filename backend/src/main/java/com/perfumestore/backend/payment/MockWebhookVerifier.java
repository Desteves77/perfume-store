package com.perfumestore.backend.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.perfumestore.backend.config.PaymentProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;

import org.springframework.stereotype.Component;

@Component
public class MockWebhookVerifier {
    private final PaymentProperties properties;

    public MockWebhookVerifier(PaymentProperties properties) { this.properties = properties; }

    public void verify(String body, String signature, String timestamp) {
        properties.validate();
        if (signature == null || signature.isBlank() || timestamp == null || timestamp.isBlank()) {
            throw invalid();
        }
        long epoch;
        try { epoch = Long.parseLong(timestamp); } catch (NumberFormatException exception) { throw invalid(); }
        if (Math.abs(Instant.now().getEpochSecond() - epoch) > properties.getWebhookTolerance().getSeconds()) {
            throw new ApiRequestException(401, "INVALID_WEBHOOK", "A assinatura do evento não é válida ou expirou.");
        }
        byte[] expected = hmac(timestamp + "." + body, properties.getMockWebhookSecret());
        String received = signature.startsWith("sha256=") ? signature.substring(7) : signature;
        byte[] provided;
        try { provided = HexFormat.of().parseHex(received); } catch (IllegalArgumentException exception) { throw invalid(); }
        if (!MessageDigest.isEqual(expected, provided)) throw invalid();
    }

    public String payloadHash(String body) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    public String sign(String body, String timestamp) {
        return "sha256=" + HexFormat.of().formatHex(hmac(timestamp + "." + body, properties.getMockWebhookSecret()));
    }

    private byte[] hmac(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private ApiRequestException invalid() {
        return new ApiRequestException(401, "INVALID_WEBHOOK", "A assinatura do evento não é válida ou expirou.");
    }
}
