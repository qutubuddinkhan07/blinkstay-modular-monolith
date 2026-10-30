package blinkstay.payment.dto;

import blinkstay.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminPaymentDto {
    private UUID paymentId;
    private UUID bookingId;
    private String bookingReference;
    private UUID guestId;
    private String provider;
    private String providerPaymentId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private Integer refundAttempts;
    private String refundError;
    private String reviewReason;
    private Instant updatedAt;
}
