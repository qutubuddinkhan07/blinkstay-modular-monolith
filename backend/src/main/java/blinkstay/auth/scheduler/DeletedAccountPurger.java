package blinkstay.auth.scheduler;

import blinkstay.auth.entities.User;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.service.ImageUploadService;
import blinkstay.common.cache.AfterCommitCacheEvictor;
import blinkstay.common.events.DeletedAccountPurgedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Purges ONE account in its own transaction (a separate bean, so @Transactional really applies).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeletedAccountPurger {
    private final UserRepository userRepository;

    @Qualifier("authImageUploadService")
    private final ImageUploadService imageUploadService;

    private final ApplicationEventPublisher publisher;
    private final AfterCommitCacheEvictor cacheEvictor;

    @Transactional
    public boolean purge(UUID userId, LocalDateTime cutoff) {
        User user = userRepository.findByIdForUpdate(userId).orElse(null);

        // re-check under the lock: an admin may have restored the account meanwhile
        if (user == null || user.getDeletedAt() == null || user.getPurgedAt() != null
                || user.getDeletedAt().isAfter(cutoff)) {
            return false;
        }

        String publicId = user.getImagePublicId();
        user.setProfileImgUrl(null);
        user.setImagePublicId(null);
        user.setPurgedAt(LocalDateTime.now());
        userRepository.save(user);

        // the review module deletes this user's reviews in this same transaction
        publisher.publishEvent(new DeletedAccountPurgedEvent(userId));
        cacheEvictor.clear("usersById", "usersByEmail", "authUsers");

        // delete the Cloudinary file only after the commit: worst case is an orphan file
        if (publicId != null && !publicId.isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        imageUploadService.deleteImage(publicId);
                    } catch (Exception e) {
                        log.error("Purge: could not delete Cloudinary image {} of user {}", publicId, userId, e);
                    }
                }
            });
        }
        return true;
    }
}