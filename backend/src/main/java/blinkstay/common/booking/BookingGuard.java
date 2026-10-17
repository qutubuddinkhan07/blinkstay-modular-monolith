package blinkstay.common.booking;

import java.util.UUID;

/**
 * "Active" means CONFIRMED, or PENDING_PAYMENT with an unexpired hold, and not
 * yet checked out.
 */
public interface BookingGuard {
	boolean listingHasActiveBookings(UUID listingId);

	boolean roomHasActiveBookings(UUID roomId);

	/** The most rooms of this type booked on any single night from today onward. */
	int peakRoomsBooked(UUID roomId);

	boolean guestHashActiveBookings(UUID userId);

	boolean hostHasActiveBookings(UUID hostId);
}
