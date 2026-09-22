package blinkstay.room.service;

import java.util.UUID;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomSummaryDto;

public interface RoomService {
	String createRoom(UUID litingId, AddRoomDto addRoomDto);

	RoomSummaryDto getRoomSummary(UUID listingId);

	boolean checkIfSameManger(UUID userId, UUID listingId);
}
