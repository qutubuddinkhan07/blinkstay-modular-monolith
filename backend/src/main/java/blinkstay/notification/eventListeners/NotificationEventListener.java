package blinkstay.notification.eventListeners;

import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import blinkstay.common.directory.UserContact;
import blinkstay.common.directory.UserDirectory;
import blinkstay.common.events.HotelManagerRequestReviewedEvent;
import blinkstay.common.events.HotelManagerRequestSubmittedEvent;
import blinkstay.common.events.ListingRestoredEvent;
import blinkstay.common.events.ListingSuspendEvent;
import blinkstay.common.events.UserBlockedEvent;
import blinkstay.common.events.UserDeletedEvent;
import blinkstay.common.events.UserUnblockedEvent;
import blinkstay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
	private final UserDirectory userDirectory;
	private final NotificationService notificationService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(UserDeletedEvent e) {
		contact(e.userId()).ifPresent(c -> notificationService.sendAccountDeleted(c.username(), c.email()));
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(UserBlockedEvent e) {
		contact(e.userId()).ifPresent(c -> notificationService.sendAccountBlocked(c.username(), c.email(), e.reason()));
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(UserUnblockedEvent e) {
		contact(e.userId()).ifPresent(c -> notificationService.sendAccountUnblocked(c.username(), c.email()));
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(HotelManagerRequestSubmittedEvent e) {
		// every active admin, except the applicant if they happen to be an admin
		for (UserContact admin : userDirectory.findActiveAdmins()) {
			if (!admin.id().equals(e.userId())) {
				notificationService.sendHostRequestSubmitted(admin.username(), admin.email(), e.username());
			}
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(HotelManagerRequestReviewedEvent e) {
		contact(e.userId()).ifPresent(c -> {
			if (e.approved()) {
				notificationService.sendHostRequestApproved(c.username(), c.email());
			} else {
				notificationService.sendHostRequestRejected(c.username(), c.email(), e.reason());
			}
		});
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(ListingSuspendEvent e) {
		contact(e.managerId()).ifPresent(
				c -> notificationService.sendListingSuspended(c.username(), c.email(), e.title(), e.reason()));
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(ListingRestoredEvent e) {
		contact(e.managerId()).ifPresent(
				c -> notificationService.sendListingRestored(c.username(), c.email(), e.title(), e.restoredStatus()));
	}

	private Optional<UserContact> contact(UUID userId) {
		return userDirectory.findContact(userId);
	}
}
