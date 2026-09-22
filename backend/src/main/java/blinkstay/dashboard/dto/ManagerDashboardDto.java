package blinkstay.dashboard.dto;

import java.util.List;

import blinkstay.listing.dto.ListingSummaryDto;
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
public class ManagerDashboardDto {
	private int totalListings;
	private int publishedListings;
	private int draftListings;
	private int totalRooms;
	private int availableRooms;

	private List<ListingSummaryDto> listings;
}
