package blinkstay.common.stats;

import java.util.Map;

public interface ListingStats {
    /**
     * Listings per status name. Every status is present.
     */
    Map<String, Long> countByStatus();
}