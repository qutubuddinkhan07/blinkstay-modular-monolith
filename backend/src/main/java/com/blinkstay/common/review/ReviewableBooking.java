package com.blinkstay.common.review;

import java.util.UUID;

/**
 * What the review module may know about a booking.
 */
public record ReviewableBooking(UUID bookingId, UUID guestId, UUID hostId, UUID listingId, String listingTitle,
                                boolean completed) {
}
