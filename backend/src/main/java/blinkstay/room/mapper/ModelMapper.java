package blinkstay.room.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.entities.ListingRoom;

@Component("roomModelMapper")
public class ModelMapper {
	public ListingRoom addRoomToListingRoom(UUID listingId, AddRoomDto dto) {
		return ListingRoom.builder().listingId(listingId).roomType(dto.getRoomType()).price(dto.getPrice())
				.totalRooms(dto.getTotalRooms()).availableRooms(dto.getAvailableRooms()).build();
	}
}
