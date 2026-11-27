package com.blinkstay.booking.service;

import com.blinkstay.booking.dto.BookingResponseDto;
import com.blinkstay.booking.dto.CancellationPreviewDto;

import java.util.UUID;

public interface BookingCancellationService {
    CancellationPreviewDto preview(UUID guestId, UUID bookingId);

    BookingResponseDto cancelByGuest(UUID guestId, UUID bookingId, String reason);

    BookingResponseDto cancelByHost(UUID hostId, UUID bookingId, String reason);

    BookingResponseDto cancelByAdmin(UUID adminId, UUID bookingId, String reason);
}
