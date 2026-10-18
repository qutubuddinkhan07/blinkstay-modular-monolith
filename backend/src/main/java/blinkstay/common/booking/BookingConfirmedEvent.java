package blinkstay.common.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingConfirmedEvent(UUID bookingId, String reference, UUID guestId, UUID hostId, UUID listingId,
		String listingTitle, String roomType, LocalDate checkIn, LocalDate checkOut, int nights, int rooms, int guests,
		BigDecimal totalAmount, String currency) {
}
