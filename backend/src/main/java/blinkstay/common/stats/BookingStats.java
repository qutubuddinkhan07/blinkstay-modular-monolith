package blinkstay.common.stats;

import java.time.LocalDate;
import java.util.Map;

public interface BookingStats {
    /**
     * Bookings created in [from, to] (hotel-zone days), counted per status name. Every status is present.
     */
    Map<String, Long> countByStatus(LocalDate from, LocalDate to);
}
