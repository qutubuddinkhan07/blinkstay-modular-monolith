package com.blinkstay.booking.service;

import com.blinkstay.booking.config.BookingProperties;
import com.blinkstay.booking.entities.Booking;
import com.blinkstay.booking.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CancellationPolicy {
    private final BookingProperties props;
    private final BookingRules rules;

    /**
     * Free cancellation ends this long before the start of the check-in day (hotel time zone).
     */
    public Instant freeCancellationUntil(Booking b) {
        return b.getCheckIn().atStartOfDay(props.zoneId()).toInstant()
                .minus(Duration.ofHours(props.getFreeCancellationHours()));
    }

    public boolean guestCanCancel(Booking b) {
        return switch (b.getStatus()) {
            case PENDING_PAYMENT -> true;
            case CONFIRMED -> rules.today().isBefore(b.getCheckIn());
            default -> false;
        };
    }

    public BigDecimal guestRefund(Booking b, Instant now) {
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            return BigDecimal.ZERO;
        }

        return now.isBefore(freeCancellationUntil(b)) ? b.getTotalAmount() : BigDecimal.ZERO;
    }
}
