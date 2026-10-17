package blinkstay.booking.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.booking.config.BookingProperties;
import blinkstay.booking.entities.Booking;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingRules {
	private final BookingProperties props;

	public LocalDate today() {
		return LocalDate.now(props.zoneId());
	}

	/** Validates the request and returns the number of nights. */
	public int validateStay(LocalDate checkIn, LocalDate checkOut, Integer rooms, Integer guests) {
		if (checkIn == null || checkOut == null) {
			throw bad("Check-in and check-out dates are required");
		}
		if (rooms == null || rooms < 1 || rooms > 10) {
			throw bad("Rooms must be between 1 and 10");
		}
		if (guests == null || guests < 1 || guests > 50) {
			throw bad("Guests must be between 1 and 50");
		}
		LocalDate today = today();
		if (checkIn.isBefore(today)) {
			throw bad("Check-in cannot be in the past");
		}
		if (!checkOut.isAfter(checkIn)) {
			throw bad("Check-out must be after check-in");
		}
		if (checkIn.isAfter(today.plusDays(props.getMaxAdvanceDays()))) {
			throw bad("Bookings open up to " + props.getMaxAdvanceDays() + " days ahead");
		}
		long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
		if (nights > props.getMaxNights()) {
			throw bad("The maximum stay is " + props.getMaxNights() + " nights");
		}
		return (int) nights;
	}

	/** The most rooms in use on any single night in [from, to). */
	public static int peakUsage(List<Booking> bookings, LocalDate from, LocalDate to) {
		int peak = 0;
		for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
			int used = 0;
			for (Booking b : bookings) {
				if (!d.isBefore(b.getCheckIn()) && d.isBefore(b.getCheckOut())) {
					used += b.getRoomsBooked();
				}
			}
			peak = Math.max(peak, used);
		}
		return peak;
	}

	private static ResponseStatusException bad(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}