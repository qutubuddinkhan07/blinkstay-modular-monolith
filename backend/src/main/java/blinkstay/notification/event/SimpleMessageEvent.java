package blinkstay.notification.event;

import blinkstay.notification.util.EmailContent;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleMessageEvent {
    private String receiverId;
    private String subject;
    /**
     * Plain-text version: printed in console mode and sent as the text part.
     */
    private String message;
    /**
     * Branded HTML version. Null means a plain-text-only email.
     */
    private String html;

    /**
     * Keeps old code that used new SimpleMessageEvent(to, subject, message) compiling.
     */
    public SimpleMessageEvent(String receiverId, String subject, String message) {
        this(receiverId, subject, message, null);
    }

    /**
     * Lombok fills in the rest of this builder; we only add content(...).
     */
    public static class SimpleMessageEventBuilder {
        /**
         * Sets subject, plain text and HTML in one go from an EmailMessageBuilderUtil method.
         */
        public SimpleMessageEventBuilder content(EmailContent content) {
            this.subject = content.subject();
            this.message = content.text();
            this.html = content.html();
            return this;
        }
    }
}