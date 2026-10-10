package com.perfumestore.backend.notification;

import com.perfumestore.backend.config.PasswordRecoveryProperties;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final PasswordRecoveryProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, PasswordRecoveryProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendPasswordRecovery(String recipient, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getSenderAddress());
        message.setTo(recipient);
        message.setSubject("Recuperação de acesso — PerfumeStore");
        message.setText("Olá,\n\nUse o link abaixo para redefinir sua senha:\n" + resetLink
                + "\n\nSe você não solicitou a recuperação, ignore esta mensagem.\n");
        mailSender.send(message);
    }
}
