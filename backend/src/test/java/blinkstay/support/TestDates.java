package blinkstay.support;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * "Today" as BlinkStay sees it (hotel time zone, app.booking.zone).
 * BookingRules reads the real clock, so tests build dates relative to this
 * (for example TestDates.today().plusDays(10)). See backlog item 11.
 */
public class TestDates {
    public static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Kolkata");

    private TestDates() {
    }

    public static LocalDate today() {
        return LocalDate.now(HOTEL_ZONE);
    }
}
