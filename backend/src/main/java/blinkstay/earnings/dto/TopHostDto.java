package blinkstay.earnings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopHostDto(UUID hostId, String hostName, long bookings, BigDecimal gross, BigDecimal commission,
                         BigDecimal hostAmount) {
    public TopHostDto {
        gross = gross == null ? BigDecimal.ZERO : gross;
        commission = commission == null ? BigDecimal.ZERO : commission;
        hostAmount = hostAmount == null ? BigDecimal.ZERO : hostAmount;
    }

    // used by the query, the name is filled in afterwards
    public TopHostDto(UUID hostId, long bookings, BigDecimal gross, BigDecimal commission, BigDecimal hostAmount) {
        this(hostId, null, bookings, gross, commission, hostAmount);
    }
}
