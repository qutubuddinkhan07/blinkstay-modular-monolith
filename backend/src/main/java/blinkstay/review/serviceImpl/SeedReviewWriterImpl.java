package blinkstay.review.serviceImpl;

import blinkstay.common.cache.AfterCommitCacheEvictor;
import blinkstay.common.review.ListingRatings;
import blinkstay.common.seed.SeedReview;
import blinkstay.common.seed.SeedReviewWriter;
import blinkstay.review.entities.Review;
import blinkstay.review.repository.ReviewRepository;
import blinkstay.review.repository.ReviewRepository.RatingSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Writes seeded reviews directly: no events, so hosts get no notification emails.
 */
@Service
@RequiredArgsConstructor
public class SeedReviewWriterImpl implements SeedReviewWriter {

    private final ReviewRepository reviewRepo;
    private final ListingRatings listingRatings;
    private final AfterCommitCacheEvictor cacheEvictor;

    @Override
    @Transactional(readOnly = true)
    public boolean existsForGuests(Collection<UUID> guestIds) {
        return !guestIds.isEmpty() && reviewRepo.existsByGuestIdIn(guestIds);
    }

    @Override
    @Transactional
    public void add(SeedReview s) {
        Review review = reviewRepo.save(Review.builder()
                .bookingId(s.bookingId()).listingId(s.listingId())
                .guestId(s.guestId()).hostId(s.hostId())
                .guestName(s.guestName()).listingTitle(s.listingTitle())
                .rating(s.rating()).comment(s.comment())
                .hostReply(s.hostReply()).hostRepliedAt(s.hostRepliedAt())
                .build());
        // @CreationTimestamp sets "now"; move it back to a few days after check-out
        reviewRepo.backdate(review.getId(), s.createdAt());
    }

    @Override
    @Transactional
    public void refreshRatings(List<UUID> listingIds) {
        for (UUID listingId : listingIds) {
            if (listingRatings.lockForReview(listingId).isEmpty()) {
                continue;
            }
            RatingSummary s = reviewRepo.summary(listingId);
            int count = s.getTotal() == null ? 0 : s.getTotal().intValue();
            BigDecimal avg = (count == 0 || s.getAverage() == null) ? null
                    : BigDecimal.valueOf(s.getAverage()).setScale(2, RoundingMode.HALF_UP);
            listingRatings.updateRating(listingId, avg, count);
        }
        cacheEvictor.clear("listingById", "publishedListings");
    }
}