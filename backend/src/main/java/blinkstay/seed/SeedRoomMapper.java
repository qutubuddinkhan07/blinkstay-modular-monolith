package blinkstay.seed;

import blinkstay.room.enums.RoomCategory;

public final class SeedRoomMapper {

	private SeedRoomMapper() {
	}

	public static RoomCategory toRoomCategory(String value) {

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Room type cannot be null");
		}

		return switch (value.trim().toLowerCase()) {

		case "single" -> RoomCategory.SINGLE;

		case "double" -> RoomCategory.DOUBLE;

		case "suite" -> RoomCategory.SUITE;

		default -> throw new IllegalArgumentException("Unknown room type: " + value);
		};
	}
}