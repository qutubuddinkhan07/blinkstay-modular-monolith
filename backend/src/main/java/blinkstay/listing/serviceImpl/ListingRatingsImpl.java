package blinkstay.listing.serviceImpl;

import blinkstay.common.review.ListingRatingTarget;
import blinkstay.common.review.ListingRatings;
import blinkstay.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListingRatingsImpl implements ListingRatings {
    private final ListingRepository listingRepo;

    // MANDATORY: the lock only means something inside the review's transaction
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ListingRatingTarget> lockForReview(UUID listingId) {
        return listingRepo.findByIdForUpdate(listingId)
                .map(l -> new ListingRatingTarget(l.getId(), l.getManagerId(), l.getTitle()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateRating(UUID listingId, BigDecimal avg, int count) {
        listingRepo.updateRating(listingId, avg, count);
    }
}
