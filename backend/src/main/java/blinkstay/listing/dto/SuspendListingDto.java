package blinkstay.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuspendListingDto {
	@NotBlank(message = "A reason is required")
	@Size(max = 500, message = "Reason must be at most 500 characters")
	private String reason;
}
