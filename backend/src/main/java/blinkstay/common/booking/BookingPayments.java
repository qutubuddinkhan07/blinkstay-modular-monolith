package blinkstay.common.booking;

import java.math.BigDecimal;
import java.util.UUID;

public interface BookingPayments {
	/**
	 * Returns the booking if this guest owns it and it can still be paid. Otherwise
	 * 404 or 409.
	 */
	PayableBooking getPayableBooking(UUID guestId, UUID bookingId);

	/** Called by the payment module after a successful payment. */
	PaymentConfirmation confirmPayment(UUID bookingId, UUID paymentId, BigDecimal paidAmount, String currency);
}