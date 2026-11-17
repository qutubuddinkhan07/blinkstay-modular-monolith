package blinkstay.review.serviceImpl;

import blinkstay.common.review.ReviewCleanup;
import blinkstay.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewCleanupImpl implements ReviewCleanup {
    private final ReviewRepository reviewRepo;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForListing(UUID listingId) {
        reviewRepo.deleteAllForListing(listingId);
    }
}
