package blinkstay.booking.controller;

import blinkstay.booking.dto.*;
import blinkstay.booking.enums.BookingStatus;
import blinkstay.booking.service.BookingCancellationService;
import blinkstay.booking.service.StaffBookingService;
import blinkstay.common.dto.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v5/admin/bookings")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminBookingController {
    private final StaffBookingService staffBookings;
    private final BookingCancellationService cancellationService;

    // e.g. ?q=BS-&status=CONFIRMED,CANCELLED&guestId=...&hostId=...&from=2026-11-01&to=2026-11-30
    @GetMapping
    public ResponseEntity<BookingApiResponse<PageResult<StaffBookingDto>>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID guestId,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) List<BookingStatus> status,
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        BookingFilter f = new BookingFilter(status, listingId, from, to, sort, direction, page, size);

        return ResponseEntity.ok(BookingApiResponse.success("Bookings for your listings",
                staffBookings.adminBookings(q, guestId, hostId, f)));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingApiResponse<BookingResponseDto>> cancel(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("bookingId") UUID bookingId, @Valid @RequestBody CancelBookingDto dto) {
        BookingResponseDto res = cancellationService.cancelByAdmin(UUID.fromString(userDetails.getUsername()),
                bookingId, dto.getReason());

        return ResponseEntity.ok(BookingApiResponse.success("Booking cancelled.", res));
    }
}
