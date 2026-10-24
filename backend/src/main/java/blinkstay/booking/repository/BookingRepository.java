package blinkstay.booking.repository;

import blinkstay.booking.entities.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    // a booking counts against availability if it is confirmed, or waiting for
    // payment with an unexpired hold
    String ACTIVE = "(b.status = blinkstay.booking.enums.BookingStatus.CONFIRMED or "
            + "(b.status = blinkstay.booking.enums.BookingStatus.PENDING_PAYMENT and b.holdExpiresAt > :now))";

    @Query("select b from Booking b where b.roomId = :roomId and b.checkIn < :checkOut and b.checkOut > :checkIn and "
            + ACTIVE)
    List<Booking> findActiveOverlapping(@Param("roomId") UUID roomId, @Param("checkIn") LocalDate checkIn,
                                        @Param("checkOut") LocalDate checkOut, @Param("now") Instant now);

    @Query("select b from Booking b where b.listingId = :listingId and b.checkIn < :checkOut "
            + "and b.checkOut > :checkIn and " + ACTIVE)
    List<Booking> findActiveOverlappingForListing(@Param("listingId") UUID listingId,
                                                  @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut, @Param("now") Instant now);

    @Query("select b from Booking b where b.roomId = :roomId and b.checkOut > :today and " + ACTIVE)
    List<Booking> findActiveFromDate(@Param("roomId") UUID roomId, @Param("today") LocalDate today,
                                     @Param("now") Instant now);

    @Query("select count(b) from Booking b where b.listingId = :listingId and b.checkOut >= :today and " + ACTIVE)
    long countActiveForListing(@Param("listingId") UUID listingId, @Param("today") LocalDate today,
                               @Param("now") Instant now);

    @Query("select count(b) from Booking b where b.roomId = :roomId and b.checkOut >= :today and " + ACTIVE)
    long countActiveForRoom(@Param("roomId") UUID roomId, @Param("today") LocalDate today, @Param("now") Instant now);

    @Query("select count(b) from Booking b where b.guestId = :userId and b.checkOut >= :today and " + ACTIVE)
    long countActiveForGuest(@Param("userId") UUID userId, @Param("today") LocalDate today, @Param("now") Instant now);

    @Query("select count(b) from Booking b where b.hostId = :userId and b.checkOut >= :today and " + ACTIVE)
    long countActiveForHost(@Param("userId") UUID userId, @Param("today") LocalDate today, @Param("now") Instant now);

    @Query("select count(b) from Booking b where b.guestId = :guestId "
            + "and b.status = blinkstay.booking.enums.BookingStatus.PENDING_PAYMENT and b.holdExpiresAt > :now")
    long countPendingHolds(@Param("guestId") UUID guestId, @Param("now") Instant now);

    boolean existsByReference(String reference);

    @Modifying(clearAutomatically = true)
    @Query("update Booking b set b.status = blinkstay.booking.enums.BookingStatus.EXPIRED, "
            + "b.version = b.version + 1, b.updatedAt = :now "
            + "where b.status = blinkstay.booking.enums.BookingStatus.PENDING_PAYMENT and b.holdExpiresAt < :now")
    int expireHolds(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    @Query("select b.paymentId from Booking b where b.id = :id")
    UUID findPaymentIdById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("update Booking b set b.status = blinkstay.booking.enums.BookingStatus.COMPLETED, "
            + "b.version = b.version + 1, b.updatedAt = :now "
            + "where b.status = blinkstay.booking.enums.BookingStatus.CONFIRMED and b.checkOut < :today")
    int completeFinished(@Param("today") LocalDate today, @Param("now") Instant now);
}
