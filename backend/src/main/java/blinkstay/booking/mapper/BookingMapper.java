package blinkstay.booking.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import blinkstay.booking.dto.BookingResponseDto;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;

@Component
public class BookingMapper {
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
				.createdAt(b.getCreatedAt()).build();
	}
}
