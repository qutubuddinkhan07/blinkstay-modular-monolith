package blinkstay.room.serviceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.dto.RoomSummaryDto;
import blinkstay.room.entities.ListingRoom;
import blinkstay.room.mapper.ModelMapper;
import blinkstay.room.repository.RoomRepository;
import blinkstay.room.service.RoomService;
import blinkstay.seed.SeedRoomMapper;
import blinkstay.seed.dto.SeedRoom;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

	private final RoomRepository roomRepo;

	@Qualifier("roomModelMapper")
	private final ModelMapper modelMapper;

	@Override
	@CacheEvict(value = "listingById", key = "#listingId")
	public String createRoom(UUID listingId, AddRoomDto addRoomDto) {
		if (roomRepo.existsByListingIdAndRoomType(listingId, addRoomDto.getRoomType())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This listing already has a " + addRoomDto.getRoomType() + " room. Edit that room instead.");
		}

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
	@CacheEvict(value = "listingById", key = "#listingId")
	public String updateRoom(UUID listingId, UUID roomId, AddRoomDto addRoomDto) {
		if (roomRepo.existsByListingIdAndRoomTypeAndIdNot(listingId, addRoomDto.getRoomType(), roomId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This listing already has a " + addRoomDto.getRoomType() + " room.");
		}
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

	@Override
	@CacheEvict(value = "listingId", key = "#listingId")
	public String deleteRoom(UUID listingId, UUID roomId) {
		ListingRoom room = roomRepo.findById(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No room found with " + roomId));

		if (!room.getListingId().equals(listingId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room does not belong to this listing.");
		}

		roomRepo.delete(room);
		return "Room with id: " + roomId + " deleted";
	}

	@Override
	@Transactional
	public void deleteRoomsByListingId(UUID listinId) {
		roomRepo.deleteByListingId(listinId);
	}

	@Override
	public void createSeedRooms(UUID listingId, List<SeedRoom> rooms, BigDecimal basePrice) {

		List<ListingRoom> entities = new ArrayList<>();

		for (SeedRoom seedRoom : rooms) {

			BigDecimal adjustment = seedRoom.getPriceAdjustment() == null ? BigDecimal.ZERO
					: seedRoom.getPriceAdjustment();

			BigDecimal actualPrice = basePrice.add(adjustment);

			ListingRoom room = ListingRoom.builder().listingId(listingId)
					.roomType(SeedRoomMapper.toRoomCategory(seedRoom.getType())).price(actualPrice)
					.totalRooms(seedRoom.getTotalRooms()).availableRooms(seedRoom.getAvailableRooms()).build();

			entities.add(room);
		}

		roomRepo.saveAll(entities);
	}

}