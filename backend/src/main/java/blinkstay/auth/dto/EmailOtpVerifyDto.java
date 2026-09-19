package blinkstay.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOtpVerifyDto {
    private String email;
    private String otp;
}
