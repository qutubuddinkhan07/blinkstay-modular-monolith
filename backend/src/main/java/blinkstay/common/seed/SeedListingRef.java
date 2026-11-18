package blinkstay.common.seed;

import java.util.UUID;

public record SeedListingRef(UUID listingId, UUID hostId, String title, String location, String coverImageUrl) {
}