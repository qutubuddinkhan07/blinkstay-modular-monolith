package blinkstay.earnings.listener;

import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.payment.BookingCancelledEvent;
import blinkstay.earnings.serviceImpl.EarningsLedger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EarningsEventListener {
    private final EarningsLedger ledger;

    // after the booking transaction commits: a problem here can never undo a payment or a cancel
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(BookingConfirmedEvent event) {
        try {
            ledger.recordConfirmed(event);
        } catch (Exception ex) {
            log.error("Could not record earnings for bookings {}", event.reference(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(BookingCancelledEvent event) {
        try {
            ledger.recordCancelled(event);
        } catch (Exception ex) {
            log.error("Could not update earnings for cancelled booking {}", event.reference(), ex);
        }
    }
}
