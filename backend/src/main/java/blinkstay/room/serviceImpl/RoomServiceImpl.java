package blinkstay.room.serviceImpl;

import blinkstay.common.booking.BookingGuard;
import blinkstay.common.cache.AfterCommitCacheEvictor;
import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.NewRoom;
import blinkstay.room.dto.RoomResponseDto;
import blinkstay.room.entities.ListingRoom;
import blinkstay.room.mapper.ModelMapper;
import blinkstay.room.repository.RoomRepository;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepo;

    @Qualifier("roomModelMapper")
    private final ModelMapper modelMapper;

    private final BookingGuard bookingGuard;

    private final AfterCommitCacheEvictor cacheEvictor;

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
                        .totalRooms(r.totalRooms()).maxGuests(r.maxGuests()).build())
                .toList();
        roomRepo.saveAll(entities);
    }

    @Override
    public boolean checkDoesListingHaveRooms(UUID listingId) {
        return roomRepo.countByListingId(listingId) > 0;
    }

    @Override
    @Transactional
    public String updateRoom(UUID listingId, UUID roomId, AddRoomDto addRoomDto) {
        // RULE: lock the room row FIRST (same as booking creation), so this change and a booking can never run together
        ListingRoom listingRoom = roomRepo.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No room found with " + roomId));

        if (!listingRoom.getListingId().equals(listingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room does not belong to this listing.");
        }

        if (roomRepo.existsByListingIdAndRoomTypeAndIdNot(listingId, addRoomDto.getRoomType(), roomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This listing already has a " + addRoomDto.getRoomType() + " room.");
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

        listingRoom.setPrice(addRoomDto.getPrice());
        listingRoom.setRoomType(addRoomDto.getRoomType());
        listingRoom.setTotalRooms(addRoomDto.getTotalRooms());

        if (addRoomDto.getMaxGuests() != null) {
            listingRoom.setMaxGuests(addRoomDto.getMaxGuests());
        }

        roomRepo.save(listingRoom);
        cacheEvictor.clear("listingById");

        return "Room updated";
    }

    @Override
    @Transactional
    public String deleteRoom(UUID listingId, UUID roomId) {
        // lock first: a booking being created for this room finishes before we check, or waits until we are done
        ListingRoom room = roomRepo.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No room found with " + roomId));

        if (!room.getListingId().equals(listingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room does not belong to this listing.");
        }

        if (bookingGuard.roomHasActiveBookings(roomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This room has upcoming bookings and cannot be deleted");
        }

        roomRepo.delete(room);
        cacheEvictor.clear("listingById");
        return "Room with id: " + roomId + " deleted";
    }

    @Override
    public List<RoomResponseDto> getRoomsByListingId(UUID listingId) {
        List<ListingRoom> rooms = roomRepo.findAllByListingId(listingId);
        return modelMapper.listingRoomToRoomResponseDto(rooms);
    }

    @Override
    @Transactional
    public void deleteRoomsByListingId(UUID listingId) {
        roomRepo.deleteByListingId(listingId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public RoomResponseDto lockRoom(UUID roomId) {
        ListingRoom room = roomRepo.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        return modelMapper.listingRoomToRoomResponseDto(List.of(room)).get(0);
    }
}