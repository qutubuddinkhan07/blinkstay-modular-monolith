package blinkstay.review.repository;

import blinkstay.review.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByBookingId(UUID bookingId);

    Optional<Review> findByBookingId(UUID bookingId);

    Page<Review> findByListingIdAndHiddenFalse(UUID listingId, Pageable pageable);

    Page<Review> findByHostIdAndHiddenFalse(UUID hostId, Pageable pageable);

    // hidden reviews never count towards the average
    @Query("SELECT avg(r.rating) AS average, count(r) AS total FROM Review r WHERE r.listingId = :listingId AND r.hidden = false")
    RatingSummary summary(@Param("listingId") UUID listingId);

    // sorted, so every caller locks the listings in the same order (no deadlock)
    @Query("SELECT DISTINCT r.listingId FROM Review r WHERE r.guestId = :guestId ORDER BY r.listingId")
    List<UUID> listingIdsByGuest(@Param("guestId") UUID guestId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Review r SET r.hidden = :hidden WHERE r.guestId = :guestId")
    int setHiddenForGuest(@Param("guestId") UUID guestId, @Param("hidden") boolean hidden);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.guestId = :guestId")
    int deleteAllForGuest(@Param("guestId") UUID guestId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.listingId = :listingId")
    int deleteAllForListing(@Param("listingId") UUID listingId);

    // seed: skip when the demo guests already have reviews
    boolean existsByGuestIdIn(Collection<UUID> guestIds);

    // seed: give a seeded review a realistic date
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Review r SET r.createdAt = :at WHERE r.id = :id")
    int backdate(@Param("id") UUID id, @Param("at") Instant at);

    interface RatingSummary {
        Double getAverage();

        Long getTotal();
    }
}