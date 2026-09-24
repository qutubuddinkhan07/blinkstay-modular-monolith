package blinkstay.seed;

import blinkstay.listing.enums.ListingCategory;

public final class SeedEnumMapper {

	private SeedEnumMapper() {
	}

	public static ListingCategory toCategory(String value) {

		if (value == null || value.isBlank()) {
			return null;
		}

		return switch (value.trim().toLowerCase()) {

		case "rooms" -> ListingCategory.ROOMS;

		case "farms" -> ListingCategory.FARMS;

		case "mountains" -> ListingCategory.MOUNTAINS;

		case "beach" -> ListingCategory.BEACH;

		case "trending" -> ListingCategory.TRENDING;

		case "domes" -> ListingCategory.DOMES;

		case "castles" -> ListingCategory.CASTLES;

		case "boats" -> ListingCategory.BOATS;

		case "arctic" -> ListingCategory.ARCTIC;

		case "iconic cities" -> ListingCategory.ICONIC_CITIES;

		default -> throw new IllegalArgumentException("Unknown listing category: " + value);
		};
	}
}