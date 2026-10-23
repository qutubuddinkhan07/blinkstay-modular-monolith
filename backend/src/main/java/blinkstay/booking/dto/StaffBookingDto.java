package blinkstay.booking.dto;

import blinkstay.booking.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffBookingDto {
    private UUID id;
    private String reference;
    private UUID listingId;
    private String listingTitle;
    private String roomType;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int nights;
    private int rooms;
    private int guests;
    private BigDecimal totalAmount;
    private String currency;
    private BookingStatus status;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancelledByRole;
    private String cancellationReason;
    private BigDecimal refundAmount;
    private boolean cancellable; // the caller (host or admin) can cancel it now
    private String guestName; // host sees the name only
    private String guestEmail; // admin only, null for hosts
    private String hostName; // admin only
}
