package blinkstay.earnings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingEarningsDto(UUID listingId, String listingTitle, long bookings, BigDecimal gross,
                                 BigDecimal hostAmount) {
    public ListingEarningsDto {
        gross = gross == null ? BigDecimal.ZERO : gross;
        hostAmount = hostAmount == null ? BigDecimal.ZERO : hostAmount;
    }
}
