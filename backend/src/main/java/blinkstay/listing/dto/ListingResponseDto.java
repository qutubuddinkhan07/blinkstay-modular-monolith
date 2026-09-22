package blinkstay.listing.dtos;

import java.util.List;

import blinkstay.listing.enums.ListingCategory;
import blinkstay.listing.enums.ListingStatus;
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
public class ListingResponseDto {

	private String title;

	private String location;

	private String description;

	private String country;

	private List<String> amenities;

	private ListingStatus status;

	private ListingCategory category;

	private String owner;
}
