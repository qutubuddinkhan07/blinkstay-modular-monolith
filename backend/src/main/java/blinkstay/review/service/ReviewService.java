package blinkstay.review.service;

import blinkstay.review.dto.ReviewPage;
import blinkstay.review.dto.ReviewRequest;
import blinkstay.review.dto.ReviewResponse;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse create(UUID guestId, UUID bookingId, ReviewRequest request);

    /**
     * The guest's review of this booking, or null if there is none yet.
     */
    ReviewResponse getMine(UUID guestId, UUID bookingId);

    ReviewResponse update(UUID guestId, UUID reviewId, ReviewRequest request);

    void delete(UUID guestId, UUID reviewId);

    void deleteAsAdmin(UUID reviewId);

    ReviewResponse reply(UUID hostId, UUID reviewId, String reply);

    ReviewResponse deleteReply(UUID hostId, UUID reviewId);

    ReviewPage listForListing(UUID listingId, int page, int size);

    ReviewPage listForHost(UUID hostId, int page, int size);
}