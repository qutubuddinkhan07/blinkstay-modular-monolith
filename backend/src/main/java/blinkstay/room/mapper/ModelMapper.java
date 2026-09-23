package blinkstay.room.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.entities.ListingRoom;

@Component("roomModelMapper")
public class ModelMapper {
	public ListingRoom addRoomToListingRoom(UUID listingId, AddRoomDto dto) {
		return ListingRoom.builder().listingId(listingId).roomType(dto.getRoomType()).price(dto.getPrice())
				.totalRooms(dto.getTotalRooms()).availableRooms(dto.getAvailableRooms()).build();
	}

	public List<RoomResponseDto> listingRoomToRoomResponseDto(List<ListingRoom> rooms) {
		return rooms.stream()
				.map(room -> RoomResponseDto.builder().roomId(room.getId()).listingId(room.getListingId())
						.roomType(room.getRoomType()).price(room.getPrice()).totalRooms(room.getTotalRooms())
						.availableRooms(room.getAvailableRooms()).build())
				.toList();
	}
}
