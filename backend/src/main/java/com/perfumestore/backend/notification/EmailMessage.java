package com.perfumestore.backend.notification;

public record EmailMessage(String kind, String recipient, String subject, String text, String html, String messageId) {
    public EmailMessage(String kind, String recipient, String subject, String text, String messageId) {
        this(kind, recipient, subject, text, "<p>" + escapeHtml(text).replace("\n", "<br>\n") + "</p>", messageId);
    }

    public EmailMessage {
        if (kind == null || kind.isBlank() || recipient == null || recipient.isBlank()
                || subject == null || subject.isBlank() || text == null || text.isBlank()
                || html == null || html.isBlank()) {
            throw new IllegalArgumentException("Mensagem de e-mail incompleta");
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
