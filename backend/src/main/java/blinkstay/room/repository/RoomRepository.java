package blinkstay.room.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blinkstay.room.entities.ListingRoom;

public interface RoomRepository extends JpaRepository<ListingRoom, UUID> {

	long countByListingId(UUID listingId);

	long countByListingIdAndAvailableRoomsGreaterThan(UUID listingId, Integer availableRooms);

	List<ListingRoom> findAllByListingId(UUID listingId);
}
