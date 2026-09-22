package blinkstay.listing.dto;

import java.util.UUID;

import blinkstay.listing.enums.ListingCategory;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.room.dto.RoomSummaryDto;
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
public class ListingSummaryDto {

	private UUID id;
	private String title;
	private String location;
	private ListingStatus status;
	private ListingCategory category;
	private String coverImage;

	private RoomSummaryDto roomSummary;
}
