package blinkstay.notification.eventListeners;

import blinkstay.notification.event.SimpleMessageEvent;
import blinkstay.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListeners {
    private final MailService mailService;

    @EventListener
    @Async
    public void handleSimpleMessageEvent(SimpleMessageEvent event) {
        log.info("SimpleMessageEvent received for {}",
                event.getReceiverId());

        mailService.sentEmail(event.getReceiverId(), event.getMessage(), event.getSubject());
    }
}
