package blinkstay.booking.dto;

import blinkstay.booking.enums.BookingStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A booking shown on the host calendar. Only the guest's name, never email or phone.
 */
public record CalendarBookingDto(UUID bookingId, String reference, UUID roomId, String roomType, String guestName,
                                 LocalDate checkIn, LocalDate checkOut, int roomsBooked, int guests,
                                 BookingStatus status) {
}