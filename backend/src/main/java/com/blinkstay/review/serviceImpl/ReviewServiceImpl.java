package com.blinkstay.review.serviceImpl;

import com.blinkstay.common.cache.AfterCommitCacheEvictor;
import com.blinkstay.common.directory.UserContact;
import com.blinkstay.common.directory.UserDirectory;
import com.blinkstay.common.events.ReviewPostedEvent;
import com.blinkstay.common.events.ReviewRepliedEvent;
import com.blinkstay.common.review.ListingRatingTarget;
import com.blinkstay.common.review.ListingRatings;
import com.blinkstay.common.review.ReviewableBooking;
import com.blinkstay.common.review.ReviewableBookings;
import com.blinkstay.review.dto.ReviewPage;
import com.blinkstay.review.dto.ReviewRequest;
import com.blinkstay.review.dto.ReviewResponse;
import com.blinkstay.review.entities.Review;
import com.blinkstay.review.repository.ReviewRepository;
import com.blinkstay.review.repository.ReviewRepository.RatingSummary;
import com.blinkstay.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviewRepo;
    private final ReviewableBookings bookings;
    private final ListingRatings listingRatings;
    private final UserDirectory userDirectory;
    private final AfterCommitCacheEvictor cacheEvictor;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public ReviewResponse create(UUID guestId, UUID bookingId, ReviewRequest request) {
        ReviewableBooking booking = bookings.find(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.guestId().equals(guestId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the guest of this booking can review it");
        }
        if (!booking.completed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can review a stay after check-out");
        }

        // Lock the listing row FIRST: it serialises reviews of one listing, so the
        // duplicate check and the average below are never based on stale data
        ListingRatingTarget listing = listingRatings.lockForReview(booking.listingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This listing no longer exists"));

        if (reviewRepo.existsByBookingId(bookingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already reviewed this stay");
        }

        String guestName = userDirectory.findContact(guestId).map(UserContact::username).orElse("Guest");

        Review review = reviewRepo.save(Review.builder().bookingId(bookingId).listingId(booking.listingId())
                .guestId(guestId).hostId(booking.hostId()).guestName(guestName).listingTitle(listing.title())
                .rating(request.rating()).comment(clean(request.comment())).build());

        refreshRating(review.getListingId());

        publisher.publishEvent(new ReviewPostedEvent(review.getId(), review.getListingId(), review.getHostId(),
                guestName, listing.title(), review.getRating()));

        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getMine(UUID guestId, UUID bookingId) {
        ReviewableBooking booking = bookings.find(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.guestId().equals(guestId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your booking");
        }
        return reviewRepo.findByBookingId(bookingId).map(this::toResponse).orElse(null);
    }

    @Override
    @Transactional
    public ReviewResponse update(UUID guestId, UUID reviewId, ReviewRequest request) {
        Review review = find(reviewId);
        if (!review.getGuestId().equals(guestId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own review");
        }

        boolean listingExists = listingRatings.lockForReview(review.getListingId()).isPresent();

        review.setRating(request.rating());
        review.setComment(clean(request.comment()));
        review.setEditedAt(Instant.now());
        reviewRepo.save(review);

        if (listingExists) {
            refreshRating(review.getListingId());
        }
        return toResponse(review);
    }

    @Override
    @Transactional
    public void delete(UUID guestId, UUID reviewId) {
        Review review = find(reviewId);
        if (!review.getGuestId().equals(guestId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own review");
        }
        remove(review);
    }

    @Override
    @Transactional
    public void deleteAsAdmin(UUID reviewId) {
        remove(find(reviewId));
    }

    private void remove(Review review) {
        boolean listingExists = listingRatings.lockForReview(review.getListingId()).isPresent();
        reviewRepo.delete(review);
        reviewRepo.flush();
        if (listingExists) {
            refreshRating(review.getListingId());
        }
    }

    @Override
    @Transactional
    public ReviewResponse reply(UUID hostId, UUID reviewId, String reply) {
        Review review = find(reviewId);
        if (!review.getHostId().equals(hostId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only reply to reviews of your own listings");
        }

        boolean first = review.getHostReply() == null;
        review.setHostReply(reply.trim());
        review.setHostRepliedAt(Instant.now());
        reviewRepo.save(review);

        // only the first reply notifies the guest: editing a reply does not spam them
        if (first) {
            publisher.publishEvent(new ReviewRepliedEvent(review.getId(), review.getListingId(), review.getGuestId(),
                    review.getListingTitle()));
        }
        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse deleteReply(UUID hostId, UUID reviewId) {
        Review review = find(reviewId);
        if (!review.getHostId().equals(hostId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage replies on your own listings");
        }
        review.setHostReply(null);
        review.setHostRepliedAt(null);
        reviewRepo.save(review);
        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPage listForListing(UUID listingId, int page, int size) {
        Page<Review> result = reviewRepo.findByListingIdAndHiddenFalse(listingId, pageable(page, size));
        RatingSummary summary = reviewRepo.summary(listingId);
        long count = summary.getTotal() == null ? 0 : summary.getTotal();
        Double avg = count == 0 || summary.getAverage() == null ? null : round(summary.getAverage());
        return toPage(result, avg, count);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPage listForHost(UUID hostId, int page, int size) {
        Page<Review> result = reviewRepo.findByHostIdAndHiddenFalse(hostId, pageable(page, size));
        return toPage(result, null, result.getTotalElements());
    }

    @Override
    @Transactional
    public void setGuestReviewsHidden(UUID guestId, boolean hidden) {
        List<UUID> listingIds = reviewRepo.listingIdsByGuest(guestId);
        if (listingIds.isEmpty()) {
            return;
        }
        lockAll(listingIds);
        reviewRepo.setHiddenForGuest(guestId, hidden);
        listingIds.forEach(this::refreshRating);
    }

    @Override
    @Transactional
    public void deleteGuestReviews(UUID guestId) {
        List<UUID> listingIds = reviewRepo.listingIdsByGuest(guestId);
        if (listingIds.isEmpty()) {
            return;
        }
        lockAll(listingIds);
        reviewRepo.deleteAllForGuest(guestId);
        listingIds.forEach(this::refreshRating);
    }

    // the ids come sorted from the query, so the lock order is always the same
    private void lockAll(List<UUID> listingIds) {
        listingIds.forEach(listingRatings::lockForReview);
    }

    // ---- helpers ----

    private Review find(UUID reviewId) {
        return reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    /**
     * Recalculates the listing's average and count, then clears the listing caches after commit.
     */
    private void refreshRating(UUID listingId) {
        RatingSummary s = reviewRepo.summary(listingId);
        int count = s.getTotal() == null ? 0 : s.getTotal().intValue();
        BigDecimal avg = (count == 0 || s.getAverage() == null) ? null
                : BigDecimal.valueOf(s.getAverage()).setScale(2, RoundingMode.HALF_UP);
        listingRatings.updateRating(listingId, avg, count);
        cacheEvictor.clear("listingById", "publishedListings");
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private ReviewPage toPage(Page<Review> result, Double avg, long count) {
        return new ReviewPage(result.getContent().stream().map(this::toResponse).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages(), avg, count);
    }

    private String clean(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getBookingId(), r.getListingId(), r.getListingTitle(),
                r.getGuestName(), r.getRating(), r.getComment(), r.getCreatedAt(), r.getEditedAt(), r.getHostReply(),
                r.getHostRepliedAt());
    }
}