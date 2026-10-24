package blinkstay.room.mapper;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.entities.ListingRoom;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("roomModelMapper")
public class ModelMapper {
    public ListingRoom addRoomToListingRoom(UUID listingId, AddRoomDto dto) {
        return ListingRoom.builder().listingId(listingId).roomType(dto.getRoomType()).price(dto.getPrice())
                .totalRooms(dto.getTotalRooms())
                .maxGuests(dto.getMaxGuests() == null ? 2 : dto.getMaxGuests()).build();
    }

    public List<RoomResponseDto> listingRoomToRoomResponseDto(List<ListingRoom> rooms) {
        return rooms.stream()
                .map(room -> RoomResponseDto.builder().roomId(room.getId()).listingId(room.getListingId())
                        .roomType(room.getRoomType()).price(room.getPrice()).totalRooms(room.getTotalRooms())
                        .maxGuests(room.getMaxGuests()).build())
                .toList();
    }
}
