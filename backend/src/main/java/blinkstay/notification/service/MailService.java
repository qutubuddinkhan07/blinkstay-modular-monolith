package blinkstay.notification.service;

public interface MailService {

    /**
     * Sends a plain-text and HTML email. html may be null for a text-only email.
     */
    boolean sentEmail(String receiverId, String subject, String text, String html);

    /**
     * Old signature (plain text only), kept so existing callers still work.
     */
    default boolean sentEmail(String receiverId, String message, String subject) {
        return sentEmail(receiverId, subject, message, null);
    }
}