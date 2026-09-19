package blinkstay.notification.event;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleMessageEvent {
    private String receiverId;
    private String subject;
    private String message;
}
