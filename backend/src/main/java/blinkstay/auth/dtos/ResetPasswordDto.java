package blinkstay.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDto {
	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email")
	private String email;

	@NotBlank(message = "Code is required")
	@Pattern(regexp = "^\\d{6}$", message = "The code must be 6 digits")
	private String otp;

	@NotBlank(message = "New password is required")
	@Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
	private String newPassword;
}
