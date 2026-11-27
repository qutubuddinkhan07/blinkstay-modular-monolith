package com.blinkstay.listing.eventListeners;

import com.blinkstay.common.cache.AfterCommitCacheEvictor;
import com.blinkstay.common.events.*;
import com.blinkstay.listing.entities.Listing;
import com.blinkstay.listing.service.FavoriteService;
import com.blinkstay.listing.service.ListingService;
import com.blinkstay.listing.serviceImpl.ListingModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserLifecycleListener {
    private final ListingModerationService moderation;
    private final FavoriteService favoriteService;
    private final ListingService listingService;
    private final AfterCommitCacheEvictor cacheEvictor;

    // Synchronous on purpose: these run inside the account transaction, so if the
    // listing work fails, the account change rolls back with it.
    @EventListener
    public void on(UserBlockedEvent e) {
        moderation.suspendAllByManager(e.userId(), e.reason());
    }

    @EventListener
    public void on(UserUnblockedEvent e) {
        moderation.restoreAfterUnblock(e.userId());
    }

    // deleted: offline but kept for 30 days, favorites kept too, so a restore brings everything back
    @EventListener
    public void on(UserDeletedEvent e) {
        moderation.suspendAllForDeletedOwner(e.userId());
    }

    @EventListener
    public void on(UserRestoredEvent e) {
        if (!e.stillBlocked()) {
            moderation.restoreAfterOwnerRestored(e.userId());
        }
    }

    // deleted for 30 days: remove the host's listings (rooms, images, favorites, reviews) and the user's favorites
    @EventListener
    public void on(DeletedAccountPurgedEvent e) {
        UUID userId = e.userId();
        for (Listing listing : listingService.getListingsByManagerId(userId)) {
            listingService.deleteListing(userId, true, listing.getId());
        }
        favoriteService.removeAllForUser(userId);
    }

    // A host changed their name, photo or business details. They are copied into
    // the cached listing pages, so clear them (rare, so clearing everything is fine).
    @EventListener
    public void on(HostDetailsChangedEvent e) {
        cacheEvictor.clear("listingById", "publishedListings");
    }
}