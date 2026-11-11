package blinkstay.notification.util;

/**
 * One finished email: the subject, a plain-text version (printed in console
 * mode and sent as the text part) and the branded HTML version.
 */
public record EmailContent(String subject, String text, String html) {
}