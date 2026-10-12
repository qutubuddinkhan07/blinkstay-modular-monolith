package blinkstay.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordDto {
	@NotBlank(message = "Username is required")
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email")
	private String email;
}
