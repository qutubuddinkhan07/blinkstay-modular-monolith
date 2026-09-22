package blinkstay.listing.dtos;

import java.util.List;

import blinkstay.listing.enums.ListingCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AddListingDto {
	@NotBlank(message = "title cannot be empty")
	private String title;

	@NotBlank(message = "location cannot be empty")
	private String location;

	@NotBlank(message = "description cannot be empty")
	private String description;

	@NotBlank(message = "country cannot be empty")
	private String country;

	@NotEmpty(message = "amenities cannot be empty")
	private List<String> amenities;

	@NotNull(message = "category cannot be empty")
	private ListingCategory category;

}
