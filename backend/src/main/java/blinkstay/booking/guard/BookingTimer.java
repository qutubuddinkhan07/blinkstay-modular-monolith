package blinkstay.booking.guard;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import blinkstay.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingTimer {
	private final BookingService bookingService;

	@Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
	public void expireHolds() {
		int expired = bookingService.expireStaleHolds();
		if (expired > 0) {
			log.info("Expired {} unpaid booking holds", expired);
		}
	}
}
