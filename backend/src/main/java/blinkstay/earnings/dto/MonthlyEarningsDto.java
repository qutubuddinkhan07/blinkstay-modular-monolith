package blinkstay.earnings.dto;

import java.math.BigDecimal;

public record MonthlyEarningsDto(int year, int month, long bookings, BigDecimal gross, BigDecimal hostAmount) {
    public MonthlyEarningsDto {
        gross = gross == null ? BigDecimal.ZERO : gross;
        hostAmount = hostAmount == null ? BigDecimal.ZERO : hostAmount;
    }
}
