package com.perfumestore.backend.notification;

import com.perfumestore.backend.config.PasswordRecoveryProperties;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    public void send(EmailMessage email) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getSenderAddress());
            helper.setTo(email.recipient());
            helper.setSubject(email.subject());
            helper.setText(email.text(), email.html());
            mailSender.send(message);
        } catch (jakarta.mail.MessagingException exception) {
            throw new IllegalStateException("Falha ao preparar mensagem de e-mail", exception);
        }
    }
}
