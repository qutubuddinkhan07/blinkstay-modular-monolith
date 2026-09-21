package blinkstay.auth.dtos;

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
