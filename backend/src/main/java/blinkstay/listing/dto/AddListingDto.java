package blinkstay.listing.dto;

import java.util.List;

import blinkstay.listing.enums.ListingCategory;
import io.swagger.v3.oas.annotations.media.Schema;
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
	@Schema(example = "Cozy Mountain Cabin")
	private String title;

	@NotBlank(message = "location cannot be empty")
	@Schema(example = "Manali")
	private String location;

	@NotBlank(message = "description cannot be empty")
	@Schema(example = "A serene escape located in the lap of nature.")
	private String description;

	@NotBlank(message = "country cannot be empty")
	@Builder.Default
	@Schema(defaultValue = "INDIA", example = "INDIA")
	private String country = "INDIA";

	@NotEmpty(message = "amenities cannot be empty")
	@Builder.Default
	@Schema(defaultValue = "[\"WI-FI\", \"HOT-TUB\"]", example = "[\"WI-FI\", \"HOT-TUB\"]")
	private List<String> amenities = List.of("WI-FI", "HOT-TUB");

	@NotNull(message = "category cannot be empty")
	@Builder.Default
	@Schema(defaultValue = "HOTEL", example = "HOTEL")
	private ListingCategory category = ListingCategory.HOTEL;
}