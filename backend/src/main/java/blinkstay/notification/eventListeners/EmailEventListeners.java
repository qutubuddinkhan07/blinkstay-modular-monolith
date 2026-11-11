package blinkstay.notification.eventListeners;

import blinkstay.notification.event.SimpleMessageEvent;
import blinkstay.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListeners {
    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleSimpleMessageEvent(SimpleMessageEvent event) {
        mailService.sentEmail(event.getReceiverId(), event.getSubject(), event.getMessage(), event.getHtml());
    }
}