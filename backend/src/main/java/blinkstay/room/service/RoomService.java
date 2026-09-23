package blinkstay.room.service;

import java.util.List;
import java.util.UUID;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.dto.RoomSummaryDto;

public interface RoomService {
	String createRoom(UUID listingId, AddRoomDto addRoomDto);

	RoomSummaryDto getRoomSummary(UUID listingId);

	boolean checkDoesListingHaveRooms(UUID listingId);

	String updateRoom(UUID listingId, UUID roomId, AddRoomDto addRoomDto);

	List<RoomResponseDto> getRoomsByListingId(UUID listingId);
}
