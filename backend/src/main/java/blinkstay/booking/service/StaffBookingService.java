package blinkstay.booking.service;

import blinkstay.booking.dto.BookingFilter;
import blinkstay.booking.dto.StaffBookingDto;
import blinkstay.common.dto.PageResult;

import java.util.UUID;

public interface StaffBookingService {
    PageResult<StaffBookingDto> hostBookings(UUID hostId, BookingFilter f);

    PageResult<StaffBookingDto> adminBookings(String q, UUID guestId, UUID hostId, BookingFilter f);
}
