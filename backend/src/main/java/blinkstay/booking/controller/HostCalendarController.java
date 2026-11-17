package blinkstay.booking.controller;

import blinkstay.booking.dto.BookingApiResponse;
import blinkstay.booking.dto.HostCalendarDto;
import blinkstay.booking.service.HostCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v5/host/calendar")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
public class HostCalendarController {
    private final HostCalendarService calendarService;

    // e.g. /api/v5/host/calendar/{listingId}?month=2026-12 (month optional: current month)
    @GetMapping("/{listingId}")
    public ResponseEntity<BookingApiResponse<HostCalendarDto>> calendar(@AuthenticationPrincipal UserDetails ud,
                                                                        @PathVariable("listingId") UUID listingId, @RequestParam(required = false) String month) {
        return ResponseEntity.ok(BookingApiResponse.success("Calendar",
                calendarService.calendar(UUID.fromString(ud.getUsername()), listingId, month)));
    }
}