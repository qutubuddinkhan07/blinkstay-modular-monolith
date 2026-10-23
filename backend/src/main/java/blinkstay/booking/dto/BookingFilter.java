package blinkstay.booking.dto;

import blinkstay.booking.enums.BookingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingFilter(List<BookingStatus> statuses, UUID listingId, LocalDate from, LocalDate to, String sort,
                            String direction, int page, int size) {
}
