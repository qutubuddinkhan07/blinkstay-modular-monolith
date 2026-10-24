package blinkstay.booking.controller;

import blinkstay.booking.dto.AvailabilityResponse;
import blinkstay.booking.dto.BookingApiResponse;
import blinkstay.booking.dto.TonightAvailabilityResponse;
import blinkstay.booking.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

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

    @GetMapping("/{listingId}/tonight")
    public ResponseEntity<BookingApiResponse<TonightAvailabilityResponse>> tonight(
            @PathVariable("listingId") UUID listingId) {
        return ResponseEntity.ok(BookingApiResponse.success("Rooms free tonight", availabilityService.tonight(listingId)));
    }
}
