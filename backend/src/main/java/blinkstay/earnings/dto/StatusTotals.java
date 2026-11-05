package blinkstay.earnings.dto;

import blinkstay.earnings.enums.EarningsStatus;

import java.math.BigDecimal;

public record StatusTotals(EarningsStatus status, long bookings, BigDecimal gross, BigDecimal refunded,
                           BigDecimal commission, BigDecimal hostAmount) {
    public StatusTotals {
        gross = gross == null ? BigDecimal.ZERO : gross;
        refunded = refunded == null ? BigDecimal.ZERO : refunded;
        commission = commission == null ? BigDecimal.ZERO : commission;
        hostAmount = hostAmount == null ? BigDecimal.ZERO : hostAmount;
    }
}
