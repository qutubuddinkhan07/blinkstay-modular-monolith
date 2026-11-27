package com.blinkstay.common.events;

import java.util.UUID;

public record ReviewPostedEvent(UUID reviewId, UUID listingId, UUID hostId, String guestName, String listingTitle,
                                int rating) {
}
