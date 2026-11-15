package blinkstay.common.review;

import java.util.UUID;

public record ListingRatingTarget(UUID listingId, UUID hostId, String title) {
}
