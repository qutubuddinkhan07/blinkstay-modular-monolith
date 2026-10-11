package blinkstay.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostRequestDto {
	@NotBlank(message = "Business name is required")
	@Size(max = 120, message = "Business name must be at most 120 charaters")
	private String businessName;

	@NotBlank(message = "Address is required")
	@Size(max = 255, message = "Address must be at most 255 characters")
	private String address;

	@NotBlank(message = "Description is required")
	@Size(min = 20, max = 1000, message = "Description must be 20 to 1000 characters")
	private String description;

	@Size(max = 255, message = "Website must be at most 255 characters")
	@Pattern(regexp = "^$|^https?://\\S+$", message = "Website must start with http:// or https://")
	private String website;
}
