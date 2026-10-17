package blinkstay.listing.serviceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import blinkstay.listing.enums.ListingCategory;

public record ListingImportCommand(UUID managerId, String title, String description, String location, String country,
		ListingCategory category, List<String> amenities, List<String> imageUrls, BigDecimal latitude,
		BigDecimal longitude, String address) {
}
