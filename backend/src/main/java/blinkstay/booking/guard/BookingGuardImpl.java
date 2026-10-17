package blinkstay.booking.guard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import blinkstay.booking.entities.Booking;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.booking.service.BookingRules;
import blinkstay.common.booking.BookingGuard;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingGuardImpl implements BookingGuard {
	private final BookingRepository bookingRepo;
	private final BookingRules rules;

	// B0: placeholder. There are no bookings yet, so nothing is ever blocked.
	@Override
	public boolean listingHasActiveBookings(UUID listingId) {
		return bookingRepo.countActiveForListing(listingId, rules.today(), Instant.now()) > 0;
	}

	@Override
	public boolean roomHasActiveBookings(UUID roomId) {
		return bookingRepo.countActiveForRoom(roomId, rules.today(), Instant.now()) > 0;
	}

	@Override
	public int peakRoomsBooked(UUID roomId) {
		LocalDate today = rules.today();
		Map<LocalDate, Integer> usage = new HashMap<>();
		for (Booking b : bookingRepo.findActiveFromDate(roomId, today, Instant.now())) {
			LocalDate start = b.getCheckIn().isBefore(today) ? today : b.getCheckIn();
			for (LocalDate d = start; d.isBefore(b.getCheckOut()); d = d.plusDays(1)) {
				usage.merge(d, b.getRoomsBooked(), Integer::sum);
			}
		}

		return usage.values().stream().mapToInt(Integer::intValue).max().orElse(0);
	}

	@Override
	public boolean guestHashActiveBookings(UUID userId) {
		return bookingRepo.countActiveForGuest(userId, rules.today(), Instant.now()) > 0;
	}

	@Override
	public boolean hostHasActiveBookings(UUID hostId) {
		return bookingRepo.countActiveForHost(hostId, rules.today(), Instant.now()) > 0;
	}
}
