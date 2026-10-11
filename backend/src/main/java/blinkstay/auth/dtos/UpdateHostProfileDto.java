package blinkstay.auth.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHostProfileDto {
	@Size(max = 120)
	private String businessName;
	@Size(max = 255)
	private String address;
	@Size(max = 1000)
	private String description;
	@Size(max = 255)
	@Pattern(regexp = "^$|^https?://\\S+$", message = "Website must start with http:// or https://")
	private String website;
}
