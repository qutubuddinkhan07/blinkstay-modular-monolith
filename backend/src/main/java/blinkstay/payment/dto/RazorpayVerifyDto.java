package blinkstay.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RazorpayVerifyDto {
    @NotBlank
    private String razorpayPaymentId;
    @NotBlank
    private String razorpaySignature;
}
