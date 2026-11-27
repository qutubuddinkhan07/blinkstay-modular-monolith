package com.blinkstay.review.controller;

import com.blinkstay.review.dto.*;
import com.blinkstay.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v5")
@RequiredArgsConstructor
@Validated
public class ReviewController {
    private final ReviewService service;

    // ---- guest ----

    @PostMapping("/bookings/{bookingId}/review")
    public ResponseEntity<ReviewApiResponse<ReviewResponse>> create(@PathVariable UUID bookingId,
                                                                    @Valid @RequestBody ReviewRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewApiResponse.success("Review posted", service.create(me(auth), bookingId, request)));
    }

    // data is null when the guest has not reviewed this booking yet
    @GetMapping("/bookings/{bookingId}/review")
    public ResponseEntity<ReviewApiResponse<ReviewResponse>> mine(@PathVariable UUID bookingId, Authentication auth) {
        return ResponseEntity.ok(ReviewApiResponse.success("Your review", service.getMine(me(auth), bookingId)));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewApiResponse<ReviewResponse>> update(@PathVariable UUID reviewId,
                                                                    @Valid @RequestBody ReviewRequest request, Authentication auth) {
        return ResponseEntity.ok(ReviewApiResponse.success("Review updated", service.update(me(auth), reviewId, request)));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewApiResponse<Void>> delete(@PathVariable UUID reviewId, Authentication auth) {
        service.delete(me(auth), reviewId);
        return ResponseEntity.ok(ReviewApiResponse.success("Review deleted"));
    }

    // ---- public ----

    @GetMapping("/listings/{listingId}/reviews")
    public ResponseEntity<ReviewApiResponse<ReviewPage>> forListing(@PathVariable UUID listingId,
                                                                    @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ReviewApiResponse.success("Reviews", service.listForListing(listingId, page, size)));
    }

    // ---- host ----

    @GetMapping("/host/reviews")
    @PreAuthorize("hasAuthority('HOTEL_MANAGER')")
    public ResponseEntity<ReviewApiResponse<ReviewPage>> forHost(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size, Authentication auth) {
        return ResponseEntity.ok(ReviewApiResponse.success("Reviews of your listings",
                service.listForHost(me(auth), page, size)));
    }

    @PutMapping("/reviews/{reviewId}/reply")
    @PreAuthorize("hasAuthority('HOTEL_MANAGER')")
    public ResponseEntity<ReviewApiResponse<ReviewResponse>> reply(@PathVariable UUID reviewId,
                                                                   @Valid @RequestBody ReplyRequest request, Authentication auth) {
        return ResponseEntity.ok(ReviewApiResponse.success("Reply saved", service.reply(me(auth), reviewId, request.reply())));
    }

    @DeleteMapping("/reviews/{reviewId}/reply")
    @PreAuthorize("hasAuthority('HOTEL_MANAGER')")
    public ResponseEntity<ReviewApiResponse<ReviewResponse>> deleteReply(@PathVariable UUID reviewId,
                                                                         Authentication auth) {
        return ResponseEntity.ok(ReviewApiResponse.success("Reply removed", service.deleteReply(me(auth), reviewId)));
    }

    // ---- admin (the /api/v5/admin/** URL rule already requires ADMIN) ----

    @DeleteMapping("/admin/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ReviewApiResponse<Void>> adminDelete(@PathVariable UUID reviewId) {
        service.deleteAsAdmin(reviewId);
        return ResponseEntity.ok(ReviewApiResponse.success("Review deleted by admin"));
    }

    // NOTE: if your other controllers read the user id differently, copy that line here
    private UUID me(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}