package blinkstay.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What waits in memory (max 10 minutes) between "send OTP" and "verify OTP".
 * Only the BCrypt hash of the password is kept, never the plain text.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TempUserRegistrationData {
    @Builder.Default
    private final AtomicInteger attempts = new AtomicInteger();

    private String otp;
    private LocalDateTime expiryTime;
    private String username;
    private String passwordHash;
    private byte[] imageBytes;
    private String originalFilename;
    private String contentType;
}