package blinkstay.earnings.dto;

import java.math.BigDecimal;

public record PlatformMonthlyDto(int year, int month, long bookings, BigDecimal gross, BigDecimal commission) {
    public PlatformMonthlyDto {
        gross = gross == null ? BigDecimal.ZERO : gross;
        commission = commission == null ? BigDecimal.ZERO : commission;
    }
}
