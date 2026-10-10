package blinkstay.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockedUserDto {
	@NotBlank(message = "A reason is required")
	@Size(max = 500, message = "Reason must be at most 500 characters")
	private String reason;
}
