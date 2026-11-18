package blinkstay.common.seed;

import java.util.List;

/**
 * Implemented by the listing module.
 */
public interface SeedListingSource {
    List<SeedListingRef> publishedListings();
}