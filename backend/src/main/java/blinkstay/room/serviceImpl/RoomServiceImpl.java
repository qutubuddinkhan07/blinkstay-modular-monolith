package blinkstay.room.serviceImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.common.booking.BookingGuard;
import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.NewRoom;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.dto.RoomSummaryDto;
import blinkstay.room.entities.ListingRoom;
import blinkstay.room.mapper.ModelMapper;
import blinkstay.room.repository.RoomRepository;
import blinkstay.room.service.RoomService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

	private final RoomRepository roomRepo;

	@Qualifier("roomModelMapper")
	private final ModelMapper modelMapper;

	private final BookingGuard bookingGuard;

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
	@Transactional
	public void createRooms(UUID listingId, List<NewRoom> rooms) {
		List<ListingRoom> entities = rooms.stream()
				.map(r -> ListingRoom.builder().listingId(listingId).roomType(r.roomType()).price(r.price())
						.totalRooms(r.totalRooms())
						// legacy column, kept equal to totalRooms only for compatibility
						.availableRooms(r.totalRooms()).maxGuests(r.maxGuests()).build())
				.toList();
		roomRepo.saveAll(entities);
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

		int peak = bookingGuard.peakRoomsBooked(roomId);
		if (addRoomDto.getTotalRooms() < peak) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Upcoming bookings need at least " + peak + " rooms of this type");
		}

		if (addRoomDto.getRoomType() != listingRoom.getRoomType() && bookingGuard.roomHasActiveBookings(roomId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The room type cannot change while it has upcoming bookings");
		}

		listingRoom.setAvailableRooms(addRoomDto.getTotalRooms());
		listingRoom.setPrice(addRoomDto.getPrice());
		listingRoom.setRoomType(addRoomDto.getRoomType());
		listingRoom.setTotalRooms(addRoomDto.getTotalRooms());

		if (addRoomDto.getMaxGuests() != null) {
			listingRoom.setMaxGuests(addRoomDto.getMaxGuests());
		}

		roomRepo.save(listingRoom);

		return "Room updated";
	}

	@Override
	public List<RoomResponseDto> getRoomsByListingId(UUID listingId) {
		List<ListingRoom> rooms = roomRepo.findAllByListingId(listingId);
		return modelMapper.listingRoomToRoomResponseDto(rooms);
	}

	@Override
	@CacheEvict(value = "listingById", key = "#listingId")
	public String deleteRoom(UUID listingId, UUID roomId) {
		ListingRoom room = roomRepo.findById(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No room found with " + roomId));

		if (!room.getListingId().equals(listingId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room does not belong to this listing.");
		}

		if (bookingGuard.roomHasActiveBookings(roomId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This room has upcoming bookings cannot be deleted");
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
	@Transactional(Transactional.TxType.MANDATORY)
	public RoomResponseDto lockRoom(UUID roomId) {
		ListingRoom room = roomRepo.findByIdForUpdate(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
		return modelMapper.listingRoomToRoomResponseDto(List.of(room)).get(0);
	}

}