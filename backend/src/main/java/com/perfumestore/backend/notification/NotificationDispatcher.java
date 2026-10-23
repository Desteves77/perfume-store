package com.perfumestore.backend.notification;

import java.time.Instant;
import java.util.Optional;

import com.perfumestore.backend.config.NotificationProperties;

import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDispatcher.class);
    private final NotificationRepository notifications;
    private final EmailSender sender;
    private final NotificationProperties properties;

    public NotificationDispatcher(NotificationRepository notifications, EmailSender sender,
            NotificationProperties properties) {
        this.notifications = notifications;
        this.sender = sender;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${NOTIFICATION_DISPATCH_INTERVAL_MS:30000}")
    public void scheduledDispatch() {
        if (properties.isEnabled() && properties.isDispatchEnabled()) dispatchOnce();
    }

    public int dispatchOnce() {
        if (!properties.isEnabled()) return 0;
        properties.validate();
        int processed = 0;
        for (int i = 0; i < properties.getBatchSize(); i++) {
            Optional<NotificationRecord> claimed = notifications.claimDue(Instant.now(),
                    Instant.now().plus(properties.getLeaseDuration()));
            if (claimed.isEmpty()) break;
            NotificationRecord notification = claimed.get();
            try {
                sender.send(new EmailMessage(notification.eventType().name(), notification.recipientEmail(),
                        notification.subject(), notification.bodyText(), notification.bodyHtml(), notification.id().toString()));
                notifications.markSent(notification.id(), notification.attemptCount(), Instant.now());
            } catch (RuntimeException exception) {
                LOGGER.warn("Falha sanitizada no envio da notificacao {}: {}", notification.id(),
                        exception.getMessage());
                boolean terminal = notification.attemptCount() >= properties.getMaxAttempts();
                notifications.markFailure(notification.id(), notification.attemptCount(),
                        "EMAIL_DELIVERY_FAILED", nextAttempt(notification.attemptCount()), terminal, Instant.now());
            }
            processed++;
        }
        return processed;
    }

    private Instant nextAttempt(int attempt) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 10);
        java.time.Duration delay = properties.getInitialBackoff().multipliedBy(multiplier);
        if (delay.compareTo(properties.getMaxBackoff()) > 0) delay = properties.getMaxBackoff();
        return Instant.now().plus(delay);
    }
}
