package blinkstay.common.events;

import java.util.UUID;

public record ReviewRepliedEvent(UUID reviewId, UUID listingId, UUID guestId, String listingTitle) {
}
