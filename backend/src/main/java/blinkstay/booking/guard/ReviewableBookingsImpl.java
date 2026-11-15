package blinkstay.booking.guard;

import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.common.review.ReviewableBooking;
import blinkstay.common.review.ReviewableBookings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewableBookingsImpl implements ReviewableBookings {
    private final BookingRepository bookingRepo;

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewableBooking> find(UUID bookingId) {
        return bookingRepo.findById(bookingId).map(b -> new ReviewableBooking(b.getId(), b.getGuestId(), b.getHostId(),
                b.getListingId(), b.getListingTitle(), b.getStatus() == BookingStatus.COMPLETED));
    }
}
