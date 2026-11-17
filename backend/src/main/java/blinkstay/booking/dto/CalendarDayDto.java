package blinkstay.booking.dto;

import java.time.LocalDate;

/**
 * One room type on one night: booked (paid), held (unpaid, hold still valid), free, and whether the day is past.
 */
public record CalendarDayDto(LocalDate date, int booked, int held, int available, boolean past) {
}