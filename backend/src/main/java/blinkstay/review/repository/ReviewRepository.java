package blinkstay.review.repository;

import blinkstay.review.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByBookingId(UUID bookingId);

    Optional<Review> findByBookingId(UUID bookingId);

    Page<Review> findByListingId(UUID listingId, Pageable pageable);

    Page<Review> findByHostId(UUID hostId, Pageable pageable);

    @Query("SELECT avg(r.rating) AS average, count(r) AS total FROM Review r WHERE r.listingId = :listingId")
    RatingSummary summary(@Param("listingId") UUID listingId);

    interface RatingSummary {
        Double getAverage();

        Long getTotal();
    }
}