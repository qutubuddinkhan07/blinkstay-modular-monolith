package com.blinkstay.listing.dto;

import com.blinkstay.common.directory.HostSummary;
import com.blinkstay.listing.enums.ListingCategory;
import com.blinkstay.listing.enums.ListingStatus;
import com.blinkstay.listing.enums.SuspensionSource;
import com.blinkstay.room.dto.RoomResponseDto;
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

    private Double averageRating;   // null until the first review

    private int reviewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private SuspensionSource suspensionSource;

    private String suspensionReason;

    private LocalDateTime suspendedAt;
}
