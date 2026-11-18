package blinkstay.common.seed;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Implemented by the review module.
 */
public interface SeedReviewWriter {
    boolean existsForGuests(Collection<UUID> guestIds);

    void add(SeedReview review);

    /**
     * Recalculates average and count; pass the ids sorted so locks are taken in a fixed order.
     */
    void refreshRatings(List<UUID> listingIds);
}