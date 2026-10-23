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
public class BookingResponseDto {
    private UUID id;
    private String reference;
    private UUID listingId;
    private UUID roomId;
    private String listingTitle;
    private String listingLocation;
    private String coverImageUrl;
    private String roomType;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int nights;
    private int rooms;
    private int guests;
    private BigDecimal pricePerNight;
    private BigDecimal totalAmount;
    private String currency;
    private BookingStatus status;
    private Instant holdExpiresAt;
    private boolean payable; // waiting for payment AND the hold has not run out
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private BigDecimal refundAmount;

    private boolean cancellable; // the guest can cancel it right now
    private Instant freeCancellationUntil; // only for confirmed bookings
    private String cancelledByRole; // GUEST, HOST or ADMIN, null if not cancelled

    private Instant createdAt;
}
