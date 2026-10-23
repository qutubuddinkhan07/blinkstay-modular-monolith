package blinkstay.booking.dto;

import blinkstay.booking.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPreviewDto {
    private UUID bookingId;
    private String reference;
    private BookingStatus status;
    private boolean cancellable;
    private BigDecimal refundAmount; // what the guest gets back if they cancel
    private String currency;
    private Instant freeCancellationUntil;
    private String message;
}
