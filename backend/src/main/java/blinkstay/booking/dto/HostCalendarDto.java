package blinkstay.booking.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * month looks like "2026-12"; today is the hotel's today, so the page can mark past days.
 */
public record HostCalendarDto(UUID listingId, String month, LocalDate today, List<RoomCalendarDto> rooms,
                              List<CalendarBookingDto> bookings) {
}