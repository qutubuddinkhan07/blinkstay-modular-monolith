package com.blinkstay.review.service;

import com.blinkstay.review.dto.ReviewPage;
import com.blinkstay.review.dto.ReviewRequest;
import com.blinkstay.review.dto.ReviewResponse;

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

    /**
     * Called when a guest is blocked (hidden = true) or unblocked (hidden = false).
     */
    void setGuestReviewsHidden(UUID guestId, boolean hidden);

    /**
     * Called when a guest account is deleted.
     */
    void deleteGuestReviews(UUID guestId);
}