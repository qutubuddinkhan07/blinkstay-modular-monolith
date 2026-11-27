package com.blinkstay.common.seed;

import java.time.Instant;
import java.util.UUID;

public record SeedReview(UUID bookingId, UUID listingId, UUID guestId, UUID hostId, String guestName,
                         String listingTitle, int rating, String comment, Instant createdAt,
                         String hostReply, Instant hostRepliedAt) {
}