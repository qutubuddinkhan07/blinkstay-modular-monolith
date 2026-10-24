package blinkstay.listing.dto;

import blinkstay.common.directory.HostSummary;
import blinkstay.listing.enums.ListingCategory;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.enums.SuspensionSource;
import blinkstay.room.dto.RoomResponseDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    private HostSummary host;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private SuspensionSource suspensionSource;

    private String suspensionReason;

    private LocalDateTime suspendedAt;
}
