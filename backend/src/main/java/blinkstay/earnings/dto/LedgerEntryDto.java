package blinkstay.earnings.dto;

import blinkstay.earnings.entities.EarningsEntry;
import blinkstay.earnings.enums.EarningsStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerEntryDto(UUID bookingId, String bookingReference, UUID listingId, String listingTitle,
                             LocalDate checkIn, LocalDate checkOut, String currency, BigDecimal gross,
                             BigDecimal refunded, BigDecimal commissionPercent, BigDecimal commission,
                             BigDecimal hostAmount, EarningsStatus status, Instant earnedAt, Instant createdAt) {
    public static LedgerEntryDto from(EarningsEntry e) {
        return new LedgerEntryDto(e.getBookingId(), e.getBookingReference(), e.getListingId(), e.getListingTitle(),
                e.getCheckIn(), e.getCheckOut(), e.getCurrency(), e.getGross(), e.getRefunded(),
                e.getCommissionPercent(), e.getCommission(), e.getHostAmount(), e.getStatus(), e.getEarnedAt(),
                e.getCreatedAt());
    }
}
