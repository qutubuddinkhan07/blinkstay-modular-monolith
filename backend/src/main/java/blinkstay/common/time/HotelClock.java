package blinkstay.common.time;

import java.time.LocalDate;

/* "Today" in the hotel time zone. Implemented by the booking module. */
public interface HotelClock {
    LocalDate today();
}
