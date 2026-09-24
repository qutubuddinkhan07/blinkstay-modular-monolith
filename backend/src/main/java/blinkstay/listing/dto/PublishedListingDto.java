package blinkstay.listing.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import blinkstay.listing.enums.ListingCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishedListingDto {
	private UUID id;

	private String title;

	private String location;

	private String country;

	private ListingCategory category;

	private List<String> amenities;

	private String coverImageUrl;

	private String address;

	private BigDecimal latitude;

	private BigDecimal longitude;
}
