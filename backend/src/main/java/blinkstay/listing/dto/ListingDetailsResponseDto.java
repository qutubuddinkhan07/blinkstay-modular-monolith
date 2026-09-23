package blinkstay.listing.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import blinkstay.listing.enums.ListingCategory;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.room.dto.RoomResponseDto;
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
public class ListingDetailsResponseDto {
	private UUID id;

	private String title;

	private String location;

	private String description;

	private String country;

	private List<String> amenities;

	private ListingStatus status;

	private ListingCategory category;

	private GeometryDto geometry;

	private List<ImageDto> images;

	private List<RoomResponseDto> rooms;

	private RoomSummaryDto roomSummary;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
