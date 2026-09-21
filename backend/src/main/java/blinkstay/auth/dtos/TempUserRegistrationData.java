package blinkstay.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TempUserRegistrationData {
    private String otp;
    private LocalDateTime expiryTime;
    private AddUserDto userDto;
    private byte[] imageBytes;
    private String originalFilename;
    private String contentType;
}
