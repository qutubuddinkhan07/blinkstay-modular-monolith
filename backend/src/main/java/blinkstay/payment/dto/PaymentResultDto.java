package blinkstay.payment.dto;

import blinkstay.payment.enums.PaymentStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultDto {
    private UUID paymentId;
    private UUID bookingId;
    private PaymentStatus paymentStatus;
    private String message;
}
