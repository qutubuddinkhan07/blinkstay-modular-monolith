package blinkstay.booking.controller;

import blinkstay.booking.dto.*;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.service.BookingCancellationService;
import blinkstay.booking.service.BookingService;
import blinkstay.common.dto.PageResult;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v5/bookings")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {
    private final BookingService bookingService;
    private final BookingCancellationService cancellationService;

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

    @GetMapping("/{bookingId}/cancellation-preview")
    public ResponseEntity<BookingApiResponse<CancellationPreviewDto>> preview(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(BookingApiResponse.success("Cancellation preview",
                cancellationService.preview(UUID.fromString(userDetails.getUsername()), bookingId)));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingApiResponse<BookingResponseDto>> cancel(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("bookingId") UUID bookingId, @Valid @RequestBody(required = false) CancelBookingDto dto) {
        BookingResponseDto res = cancellationService.cancelByGuest(UUID.fromString(userDetails.getUsername()),
                bookingId, dto == null ? null : dto.getReason());

        return ResponseEntity.ok(BookingApiResponse.success("Booking cancelled", res));
    }
}
