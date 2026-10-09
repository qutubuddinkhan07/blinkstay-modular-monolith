package blinkstay.auth.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileDto {
	@Size(min = 2, max = 50)
	private String username;

	@Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid phone number")
	private String phone;

	@Size(max = 300)
	private String bio;
}
