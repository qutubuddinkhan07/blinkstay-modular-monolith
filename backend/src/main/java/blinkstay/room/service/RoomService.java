package blinkstay.room.service;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.NewRoom;
import blinkstay.room.dto.RoomResponseDto;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    String createRoom(UUID listingId, AddRoomDto addRoomDto);

    void createRooms(UUID listingId, List<NewRoom> rooms);
    
    boolean checkDoesListingHaveRooms(UUID listingId);

    String updateRoom(UUID listingId, UUID roomId, AddRoomDto addRoomDto);

    List<RoomResponseDto> getRoomsByListingId(UUID listingId);

    String deleteRoom(UUID listingId, UUID roomId);

    void deleteRoomsByListingId(UUID listingId);

    RoomResponseDto lockRoom(UUID roomId);
}
