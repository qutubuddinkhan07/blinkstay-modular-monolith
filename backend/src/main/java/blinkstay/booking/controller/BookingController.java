package blinkstay.booking.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.booking.dto.BookingApiResponse;
import blinkstay.booking.dto.BookingResponseDto;
import blinkstay.booking.dto.CreateBookingDto;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.service.BookingService;
import blinkstay.common.dto.PageResult;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v5/bookings")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {
	private final BookingService bookingService;

	@PostMapping
	public ResponseEntity<BookingApiResponse<BookingResponseDto>> create(
			@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody CreateBookingDto dto) {
		BookingResponseDto res = bookingService.create(UUID.fromString(userDetails.getUsername()), dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(BookingApiResponse.success("Booking created. Complete the payment to confirm it.", res));
	}

	// e.g. ?status=PENDING_PAYMENT,CONFIRMED&sort=checkIn&direction=asc
	@GetMapping("/me")
	public ResponseEntity<BookingApiResponse<PageResult<BookingResponseDto>>> mine(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(required = false) List<BookingStatus> status,
			@RequestParam(defaultValue = "createdAt") String sort,
			@RequestParam(defaultValue = "desc") String direction, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(BookingApiResponse.success("Your bookings", bookingService
				.myBookings(UUID.fromString(userDetails.getUsername()), status, sort, direction, page, size)));
	}

	@GetMapping("/{bookingId}")
	public ResponseEntity<BookingApiResponse<BookingResponseDto>> getOne(
			@AuthenticationPrincipal UserDetails userDetails, @PathVariable("bookingId") UUID bookingId) {
		return ResponseEntity.ok(BookingApiResponse.success("Booking",
				bookingService.getMine(UUID.fromString(userDetails.getUsername()), bookingId)));
	}
}
