package com.blinkstay.booking.service;

import com.blinkstay.booking.dto.BookingFilter;
import com.blinkstay.booking.dto.StaffBookingDto;
import com.blinkstay.common.dto.PageResult;

import java.util.UUID;

public interface StaffBookingService {
    PageResult<StaffBookingDto> hostBookings(UUID hostId, BookingFilter f);

    PageResult<StaffBookingDto> adminBookings(String q, UUID guestId, UUID hostId, BookingFilter f);
}
