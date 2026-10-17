package blinkstay.booking.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.booking.dto.AvailabilityResponse;
import blinkstay.booking.dto.BookingApiResponse;
import blinkstay.booking.service.AvailabilityService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v5/availability")
@RequiredArgsConstructor
public class AvailabilityController {
	private final AvailabilityService availabilityService;

	// required=false so a missing date is a clear 400 from our own check, not an
	// unhandled error
	@GetMapping("/{listingId}")
	public ResponseEntity<BookingApiResponse<AvailabilityResponse>> availability(
			@PathVariable("listingId") UUID listingId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
			@RequestParam(defaultValue = "1") int rooms, @RequestParam(defaultValue = "2") int guests) {
		return ResponseEntity
				.ok(BookingApiResponse.<AvailabilityResponse>builder().success(true).message("Availability")
						.data(availabilityService.check(listingId, checkIn, checkOut, rooms, guests)).build());
	}
}
