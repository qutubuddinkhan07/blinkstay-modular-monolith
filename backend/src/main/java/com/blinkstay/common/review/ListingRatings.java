package com.blinkstay.common.review;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ListingRatings {
    /**
     * Locks the listing row until the transaction ends. Empty if the listing was deleted.
     */
    Optional<ListingRatingTarget> lockForReview(UUID listingId);

    /**
     * avg is null when there are no reviews.
     */
    void updateRating(UUID listingId, BigDecimal avg, int count);
}
