package com.perfumestore.backend.notification;

public interface EmailSender {

    void send(EmailMessage message);

    default void sendPasswordRecovery(String recipient, String resetLink) {
        send(new EmailMessage("PASSWORD_RECOVERY", recipient,
                "Recuperacao de acesso - PerfumeStore",
                "Ola,\n\nUse o link abaixo para redefinir sua senha:\n" + resetLink
                        + "\n\nSe voce nao solicitou a recuperacao, ignore esta mensagem.\n",
                "<p>Ola,</p><p>Use o link abaixo para redefinir sua senha:</p><p><a href=\""
                        + escapeAttribute(resetLink) + "\">Redefinir senha</a></p>"
                        + "<p>Se voce nao solicitou a recuperacao, ignore esta mensagem.</p>", null));
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
