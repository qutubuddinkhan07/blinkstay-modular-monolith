package blinkstay.room.serviceImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.dto.RoomSummaryDto;
import blinkstay.room.entities.ListingRoom;
import blinkstay.room.mapper.ModelMapper;
import blinkstay.room.repository.RoomRepository;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

	private final RoomRepository roomRepo;

	@Qualifier("roomModelMapper")
	private final ModelMapper modelMapper;

	@Override
	public String createRoom(UUID listingId, AddRoomDto addRoomDto) {

		ListingRoom listingRoom = modelMapper.addRoomToListingRoom(listingId, addRoomDto);

		ListingRoom savedRoom = roomRepo.save(listingRoom);

		return savedRoom.getId() + " room : created";
	}

	@Override
	public RoomSummaryDto getRoomSummary(UUID listingId) {

		long totalRooms = roomRepo.countByListingId(listingId);

		long availableRooms = roomRepo.countByListingIdAndAvailableRoomsGreaterThan(listingId, 0);

		return RoomSummaryDto.builder().totalRooms((int) totalRooms).availableRooms((int) availableRooms).build();
	}

	@Override
	public boolean checkDoesListingHaveRooms(UUID listingId) {
		return roomRepo.countByListingId(listingId) > 0;
	}

	@Override
	public String updateRoom(UUID listingId, UUID roomId, AddRoomDto addRoomDto) {
		ListingRoom listingRoom = roomRepo.findById(roomId)
				.orElseThrow(() -> new RuntimeException("No room found with " + roomId));
		boolean sameListing = listingRoom.getListingId().equals(listingId);

		if (!sameListing) {
			throw new RuntimeException("Listing Id not same as rooms listing id");
		}

		listingRoom.setAvailableRooms(addRoomDto.getAvailableRooms());
		listingRoom.setPrice(addRoomDto.getPrice());
		listingRoom.setRoomType(addRoomDto.getRoomType());
		listingRoom.setTotalRooms(addRoomDto.getTotalRooms());

		roomRepo.save(listingRoom);

		return "Room updated";
	}

	@Override
	public List<RoomResponseDto> getRoomsByListingId(UUID listingId) {
		List<ListingRoom> rooms = roomRepo.findAllByListingId(listingId);
		return modelMapper.listingRoomToRoomResponseDto(rooms);
	}

}