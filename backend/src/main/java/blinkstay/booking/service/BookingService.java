package blinkstay.booking.service;

import java.util.List;
import java.util.UUID;

import blinkstay.booking.dto.BookingResponseDto;
import blinkstay.booking.dto.CreateBookingDto;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.common.dto.PageResult;

public interface BookingService {
	BookingResponseDto create(UUID guestId, CreateBookingDto dto);

	PageResult<BookingResponseDto> myBookings(UUID guestId, List<BookingStatus> statues, String sort, String direction,
			int page, int size);

	BookingResponseDto getMine(UUID guestId, UUID bookingId);

	int expireStaleHolds();
}
