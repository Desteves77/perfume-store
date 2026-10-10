package com.perfumestore.backend.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class CapturedEmailSender implements EmailSender {

    private final CopyOnWriteArrayList<CapturedEmail> messages = new CopyOnWriteArrayList<>();
    private volatile boolean fail;

    @Override
    public void sendPasswordRecovery(String recipient, String resetLink) {
        if (fail) {
            throw new IllegalStateException("simulated mail delivery failure");
        }
        messages.add(new CapturedEmail(recipient, resetLink));
    }

    public List<CapturedEmail> messages() {
        return List.copyOf(messages);
    }

    public void clear() {
        messages.clear();
        fail = false;
    }

    public void failDelivery() {
        fail = true;
    }

    public record CapturedEmail(String recipient, String resetLink) { }
}
