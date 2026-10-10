package blinkstay.listing.eventListeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import blinkstay.common.events.UserBlockedEvent;
import blinkstay.common.events.UserUnblockedEvent;
import blinkstay.listing.serviceImpl.ListingModerationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserLifecycleListener {
	private final ListingModerationService moderation;

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
}
