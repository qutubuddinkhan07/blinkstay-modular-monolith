package blinkstay.room.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import blinkstay.room.entities.ListingRoom;
import blinkstay.room.enums.RoomCategory;

public interface RoomRepository extends JpaRepository<ListingRoom, UUID> {

	long countByListingId(UUID listingId);

	long countByListingIdAndAvailableRoomsGreaterThan(UUID listingId, Integer availableRooms);

	List<ListingRoom> findAllByListingId(UUID listingId);

	@Modifying
	@Query("delete from ListingRoom r where r.listingId = :listingId")
	int deleteByListingId(@Param("listingId") UUID listingId);

	boolean existsByListingIdAndRoomType(UUID listingId, RoomCategory roomType);

	boolean existsByListingIdAndRoomTypeAndIdNot(UUID listingId, RoomCategory roomType, UUID roomId);
}
