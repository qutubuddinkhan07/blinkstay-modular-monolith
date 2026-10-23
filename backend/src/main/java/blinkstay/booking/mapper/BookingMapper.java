package blinkstay.booking.mapper;

import blinkstay.booking.dto.BookingResponseDto;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.service.CancellationPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BookingMapper {
    private final CancellationPolicy policy;

    public BookingResponseDto toDto(Booking b, Instant now) {
        boolean payable = b.getStatus() == BookingStatus.PENDING_PAYMENT && b.getHoldExpiresAt() != null
                && b.getHoldExpiresAt().isAfter(now);
        return BookingResponseDto.builder().id(b.getId()).reference(b.getReference()).listingId(b.getListingId())
                .roomId(b.getRoomId()).listingTitle(b.getListingTitle()).listingLocation(b.getListingLocation())
                .coverImageUrl(b.getCoverImageUrl()).roomType(b.getRoomType()).checkIn(b.getCheckIn())
                .checkOut(b.getCheckOut()).nights(b.getNights()).rooms(b.getRoomsBooked()).guests(b.getGuests())
                .pricePerNight(b.getPricePerNight()).totalAmount(b.getTotalAmount()).currency(b.getCurrency())
                .status(b.getStatus()).holdExpiresAt(b.getHoldExpiresAt()).payable(payable)
                .confirmedAt(b.getConfirmedAt()).cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason()).refundAmount(b.getRefundAmount())
                .createdAt(b.getCreatedAt()).cancellable(policy.guestCanCancel(b))
                .freeCancellationUntil(
                        b.getStatus() == BookingStatus.CONFIRMED ? policy.freeCancellationUntil(b) : null)
                .cancelledByRole(cancelledByRole(b)).build();
    }

    public static String cancelledByRole(Booking b) {
        if (b.getCancelledBy() == null) {
            return null;
        }
        if (b.getCancelledBy().equals(b.getGuestId())) {
            return "GUEST";
        }
        return b.getCancelledBy().equals(b.getHostId()) ? "HOST" : "ADMIN";
    }
}
