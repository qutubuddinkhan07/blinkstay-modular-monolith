package blinkstay.common.events;

import java.util.UUID;

public record ListingRestoredEvent(UUID listingId, UUID managerId, String title, String restoredStatus) {
}
