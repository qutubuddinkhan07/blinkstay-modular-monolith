package blinkstay.auth.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponseDto {
    private String accessToken;
    private UserResponseDto userResponseDto;
}
