package blinkstay.earnings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EarningsSummaryDto(LocalDate from, LocalDate to, String currency, long bookings, long cancelled,
                                 BigDecimal gross, BigDecimal refunded, BigDecimal commission, BigDecimal hostAmount,
                                 BigDecimal earned, BigDecimal pending, BigDecimal averageBookingValue) {
}
