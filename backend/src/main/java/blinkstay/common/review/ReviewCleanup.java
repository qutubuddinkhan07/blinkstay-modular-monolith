package blinkstay.common.review;

import java.util.UUID;

public interface ReviewCleanup {
    /**
     * Removes every review of a listing. Call it inside the listing-delete transaction.
     */
    void deleteForListing(UUID listingId);
}
