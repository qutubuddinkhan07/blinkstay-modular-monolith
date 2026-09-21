package blinkstay.listing.dtos;

import java.util.List;

import blinkstay.listing.enums.ListingCategory;
import jakarta.validation.constraints.NotBlank;
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

	@NotBlank(message = "amenities cannot be empty")
	private List<String> amenities;

	@NotBlank(message = "category cannot be empty")
	private ListingCategory category;

}
