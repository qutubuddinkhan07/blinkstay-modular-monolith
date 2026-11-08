package blinkstay.earnings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record AdminOverviewDto(LocalDate from, LocalDate to, String currency, Money money,
                               Map<String, Long> bookingsByStatus, Users users, Map<String, Long> listingsByStatus,
                               long paymentsNeedingAttention) {
    public record Money(BigDecimal gross, BigDecimal refunded, BigDecimal commission, BigDecimal owedToHosts,
                        BigDecimal earned, BigDecimal pending, BigDecimal averageBookingValue, long paidBookings,
                        long fullyRefunded, BigDecimal cancellationRatePercent) {
    }

    public record Users(long accounts, long hosts, long admins, long blocked, long newAccounts) {
    }
}
