package blinkstay.room.serviceImpl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import blinkstay.listing.service.ListingService;
import blinkstay.room.dto.AddRoomDto;
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

	private final ListingService listingService;

	@Override
	public String createRoom(UUID litingId, AddRoomDto addRoomDto) {
		ListingRoom listingRoom = modelMapper.addRoomToListingRoom(litingId, addRoomDto);

		ListingRoom savedRoom = roomRepo.save(listingRoom);
		return savedRoom.getId() + " room : created";
	}

	@Override
	public boolean checkIfSameManger(UUID userId, UUID listingId) {
		return listingService.checkWhetherSameManager(userId, listingId);
	}

	@Override
	public RoomSummaryDto getRoomSummary(UUID listingId) {

		long totalRooms = roomRepo.countByListingId(listingId);

		long availableRooms = roomRepo.countByListingIdAndAvailableRoomsGreaterThan(listingId, 0);

		return RoomSummaryDto.builder().totalRooms((int) totalRooms).availableRooms((int) availableRooms).build();
	}

}
