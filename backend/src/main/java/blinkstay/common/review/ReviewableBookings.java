package blinkstay.common.review;

import java.util.Optional;
import java.util.UUID;

public interface ReviewableBookings {
    Optional<ReviewableBooking> find(UUID bookingId);
}
