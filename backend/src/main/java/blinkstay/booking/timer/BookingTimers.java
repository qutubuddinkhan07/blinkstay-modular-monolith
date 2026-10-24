package blinkstay.booking.timer;

import blinkstay.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingTimers {
    private final BookingService bookingService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void expireHolds() {
        int expired = bookingService.expireStaleHolds();
        if (expired > 0) {
            log.info("Expired {} unpaid booking holds", expired);
        }
    }

    // hourly by default; the property lets you shorten it while testing
    @Scheduled(fixedDelayString = "${app.booking.complete-interval-ms:3600000}", initialDelay = 60_000)
    public void completeStays() {
        int done = bookingService.completeFinishedStays();
        if (done > 0) {
            log.info("Marked {} finished stays as completed", done);
        }
    }
}