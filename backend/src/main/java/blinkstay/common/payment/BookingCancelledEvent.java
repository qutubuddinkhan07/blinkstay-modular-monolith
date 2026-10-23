package blinkstay.common.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingCancelledEvent(UUID bookingId, String reference, UUID guestId, UUID hostId, String listingTitle,
                                    LocalDate checkIn, LocalDate checkOut, String cancelledBy /* GUEST, HOST, or
                                    ADMIN*/, String reason, BigDecimal refundAmount /* null  when nothing was paid */
        , String currency, boolean wasPaid) {
}
