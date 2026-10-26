package blinkstay.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSessionDto {
    private UUID paymentId;
    private UUID bookingId;
    private String bookingReference;
    private String provider;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private boolean mock;
    private Instant holdExpiresAt;
    private String publicKey;
}
