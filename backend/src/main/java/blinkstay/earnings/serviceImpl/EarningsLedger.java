package blinkstay.earnings.serviceImpl;

import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.payment.BookingCancelledEvent;
import blinkstay.common.time.HotelClock;
import blinkstay.earnings.entities.EarningsEntry;
import blinkstay.earnings.enums.EarningsStatus;
import blinkstay.earnings.repository.EarningsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Writes the earnings ledger. REQUIRES_NEW on purpose: these methods are called AFTER the booking transaction has
 * committed, and joining a finished transaction would silently lose the write.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EarningsLedger {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final EarningsRepository repo;
    private final HotelClock clock;

    @Value("${app.earnings.commission-percent:10}")
    private BigDecimal commissionPercent;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConfirmed(BookingConfirmedEvent e) {
        if (repo.existsByBookingId(e.bookingId())) {
            return; // the same event twice creates nothing new
        }

        EarningsEntry entry = EarningsEntry.builder().bookingId(e.bookingId()).bookingReference(e.reference())
                .hostId(e.hostId()).listingId(e.listingId()).listingTitle(e.listingTitle()).checkIn(e.checkIn())
                .checkOut(e.checkOut()).currency(e.currency()).gross(e.totalAmount()).refunded(BigDecimal.ZERO)
                .commissionPercent(commissionPercent).status(EarningsStatus.PENDING).build();

        recalculate(entry);
        repo.save(entry);

        log.info("Earnings recorder for bookings {}: host gets {} {}", e.reference(), entry.getHostAmount(),
                entry.getCurrency());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCancelled(BookingCancelledEvent e) {
        if (!e.wasPaid()) {
            return; // nothing was paid, so there is no row
        }

        EarningsEntry entry = repo.findByBookingIdForUpdate(e.bookingId()).orElse(null);
        if (entry == null) {
            log.warn("No earnings row for cancelled booking {}", e.reference());
            return;
        }
        if (entry.getStatus() == EarningsStatus.REVERSED) {
            return;
        }

        BigDecimal refund = e.refundAmount() == null ? BigDecimal.ZERO : e.refundAmount();
        entry.setRefunded(refund.min(entry.getGross()));
        recalculate(entry);
        repo.save(entry);

        log.info("Earnings updated for cancelled booking {}: refunded {}, host gets {}", e.reference(),
                entry.getRefunded(), entry.getHostAmount());
    }

    /**
     * Timer: stays that are over become EARNED.
     */
    @Transactional
    public int markEarned() {
        return repo.markEarned(clock.today(), Instant.now());
    }

    private void recalculate(EarningsEntry en) {
        BigDecimal base = en.getGross().subtract(en.getRefunded());
        if (base.signum() <= 0) {
            en.setCommission(BigDecimal.ZERO.setScale(2));
            en.setHostAmount(BigDecimal.ZERO.setScale(2));
            en.setStatus(EarningsStatus.REVERSED);
            return;
        }

        BigDecimal commission = base.multiply(en.getCommissionPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        en.setCommission(commission);
        en.setHostAmount(base.subtract(commission));
    }
}
