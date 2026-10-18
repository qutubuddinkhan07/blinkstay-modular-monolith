package blinkstay.listing.eventListeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import blinkstay.common.cache.AfterCommitCacheEvictor;
import blinkstay.common.events.HostDetailsChangedEvent;
import blinkstay.common.events.UserBlockedEvent;
import blinkstay.common.events.UserDeletedEvent;
import blinkstay.common.events.UserUnblockedEvent;
import blinkstay.listing.service.FavoriteService;
import blinkstay.listing.serviceImpl.ListingModerationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserLifecycleListener {
	private final ListingModerationService moderation;
	private final FavoriteService favoriteService;
	private final AfterCommitCacheEvictor cacheEvictor;

	// Synchronous on purpose: it runs inside the block transaction, so if
	// suspending the listings fails, the block rolls back with it.
	@EventListener
	public void on(UserBlockedEvent e) {
		moderation.suspendAllByManager(e.userId(), e.reason());
	}

	@EventListener
	public void on(UserUnblockedEvent e) {
		moderation.restoreAfterUnblock(e.userId());
	}

	@EventListener
	public void on(UserDeletedEvent e) {
		moderation.suspendAllByManager(e.userId(), "Account deleted"); // same effect as before
		favoriteService.removeAllForUser(e.userId());
	}

	// A host changed their name, photo or business details. The details are copied
	// into the cached
	// listing pages, so clear them. The whole cache is cleared because this is rare
	// and finding
	// this host's listings one by one would be more work than it saves.
	public void on(HostDetailsChangedEvent e) {
		cacheEvictor.clear("listingById");
	}
}
