package com.blinkstay.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(UUID id, UUID bookingId, UUID listingId, String listingTitle, String guestName,
                             int rating, String comment, Instant createdAt, Instant editedAt, String hostReply,
                             Instant hostRepliedAt) {
}