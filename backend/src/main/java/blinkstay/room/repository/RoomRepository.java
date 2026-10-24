package blinkstay.room.repository;

import blinkstay.room.entities.ListingRoom;
import blinkstay.room.enums.RoomCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<ListingRoom, UUID> {

    long countByListingId(UUID listingId);

    List<ListingRoom> findAllByListingId(UUID listingId);

    @Modifying
    @Query("delete from ListingRoom r where r.listingId = :listingId")
    int deleteByListingId(@Param("listingId") UUID listingId);

    boolean existsByListingIdAndRoomType(UUID listingId, RoomCategory roomType);

    boolean existsByListingIdAndRoomTypeAndIdNot(UUID listingId, RoomCategory roomType, UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ListingRoom r where r.id = :id")
    Optional<ListingRoom> findByIdForUpdate(UUID id);
}
