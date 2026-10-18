package blinkstay.common.booking;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundedEvent(UUID paymentId, UUID bookingId, UUID guestId, String bookingReference,
		BigDecimal amount, String currency, String reason) {
}
