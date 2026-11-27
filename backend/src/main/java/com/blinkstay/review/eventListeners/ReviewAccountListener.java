package com.blinkstay.review.eventListeners;

import com.blinkstay.common.events.*;
import com.blinkstay.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Plain @EventListener: runs in the SAME transaction as the account change, so
 * reviews and averages change together with the account, or not at all.
 */
@Component
@RequiredArgsConstructor
public class ReviewAccountListener {
    private final ReviewService reviews;

    @EventListener
    public void on(UserBlockedEvent e) {
        reviews.setGuestReviewsHidden(e.userId(), true);
    }

    @EventListener
    public void on(UserUnblockedEvent e) {
        reviews.setGuestReviewsHidden(e.userId(), false);
    }

    // deleted: hide only, an admin can still restore the account
    @EventListener
    public void on(UserDeletedEvent e) {
        reviews.setGuestReviewsHidden(e.userId(), true);
    }

    @EventListener
    public void on(UserRestoredEvent e) {
        if (!e.stillBlocked()) {
            reviews.setGuestReviewsHidden(e.userId(), false);
        }
    }

    // deleted for 30 days: now remove them for good
    @EventListener
    public void on(DeletedAccountPurgedEvent e) {
        reviews.deleteGuestReviews(e.userId());
    }
}