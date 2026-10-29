package blinkstay.payment.listener;

import blinkstay.payment.event.RefundRequestedEvent;
import blinkstay.payment.serviceImpl.RefundProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RefundRequestListener {
    private final RefundProcessor processor;

    // runs only after the cancelling transaction has committed, on its own thread
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(RefundRequestedEvent event) {
        processor.attempt(event.paymentId());
    }
}
