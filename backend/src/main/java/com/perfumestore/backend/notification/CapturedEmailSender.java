package com.perfumestore.backend.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class CapturedEmailSender implements EmailSender {

    private final CopyOnWriteArrayList<CapturedEmail> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<EmailMessage> delivered = new CopyOnWriteArrayList<>();
    private volatile boolean fail;

    @Override
    public void send(EmailMessage message) {
        if (fail) {
            throw new IllegalStateException("simulated mail delivery failure");
        }
        delivered.add(message);
        if ("PASSWORD_RECOVERY".equals(message.kind())) {
            String marker = "Use o link abaixo para redefinir sua senha:\n";
            int start = message.text().indexOf(marker);
            String link = start < 0 ? "" : message.text().substring(start + marker.length()).split("\\n", 2)[0];
            messages.add(new CapturedEmail(message.recipient(), link));
        }
    }

    public List<CapturedEmail> messages() {
        return List.copyOf(messages);
    }

    public List<EmailMessage> deliveredMessages() {
        return List.copyOf(delivered);
    }

    public void clear() {
        messages.clear();
        delivered.clear();
        fail = false;
    }

    public void failDelivery() {
        fail = true;
    }

    public record CapturedEmail(String recipient, String resetLink) { }
}
