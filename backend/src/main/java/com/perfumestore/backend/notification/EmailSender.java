package com.perfumestore.backend.notification;

public interface EmailSender {

    void sendPasswordRecovery(String recipient, String resetLink);
}
