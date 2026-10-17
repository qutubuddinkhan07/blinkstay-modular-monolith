package blinkstay.auth.dtos;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TempUserRegistrationData {
	@Builder.Default
	private final AtomicInteger attempts = new AtomicInteger();

	private String otp;
	private LocalDateTime expiryTime;
	private AddUserDto userDto;
	private byte[] imageBytes;
	private String originalFilename;
	private String contentType;
}
