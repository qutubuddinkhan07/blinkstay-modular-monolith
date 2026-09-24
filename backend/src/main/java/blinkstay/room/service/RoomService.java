package blinkstay.room.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.dto.RoomSummaryDto;
import blinkstay.seed.dto.SeedRoom;

public interface RoomService {
	String createRoom(UUID listingId, AddRoomDto addRoomDto);

	RoomSummaryDto getRoomSummary(UUID listingId);

	boolean checkDoesListingHaveRooms(UUID listingId);

	String updateRoom(UUID listingId, UUID roomId, AddRoomDto addRoomDto);

	List<RoomResponseDto> getRoomsByListingId(UUID listingId);

	String deleteRoom(UUID roomId);

	void createSeedRooms(UUID listingId, List<SeedRoom> rooms, BigDecimal basePrice);

}
