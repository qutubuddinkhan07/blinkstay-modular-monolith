package blinkstay.listing.dto;

import java.util.UUID;

public record ListingBookingInfo(UUID id, UUID managerId, String title, String location, String coverImageUrl,
		boolean published) {

}
