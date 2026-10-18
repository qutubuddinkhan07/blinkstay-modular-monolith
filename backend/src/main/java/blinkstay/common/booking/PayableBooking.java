package blinkstay.common.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayableBooking(UUID bookingId, String reference, BigDecimal totalAmount, String currency,
		Instant holdExpiresAt) {
}