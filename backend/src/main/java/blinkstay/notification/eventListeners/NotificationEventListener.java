package blinkstay.notification.eventListeners;

import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.booking.PaymentRefundedEvent;
import blinkstay.common.directory.UserContact;
import blinkstay.common.directory.UserDirectory;
import blinkstay.common.events.*;
import blinkstay.common.payment.BookingCancelledEvent;
import blinkstay.notification.enums.NotificationType;
import blinkstay.notification.service.InAppNotificationService;
import blinkstay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final UserDirectory userDirectory;
    private final NotificationService emails;
    private final InAppNotificationService inApp;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserDeletedEvent e) {
        // email only: a deleted account can never open the app
        contact(e.userId()).ifPresent(c -> emails.sendAccountDeleted(c.username(), c.email()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserBlockedEvent e) {
        save(e.userId(), NotificationType.ACCOUNT_BLOCKED, "Your account was blocked", "Reason: " + e.reason(), null);
        contact(e.userId()).ifPresent(c -> emails.sendAccountBlocked(c.username(), c.email(), e.reason()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserUnblockedEvent e) {
        save(e.userId(), NotificationType.ACCOUNT_UNBLOCKED, "Your account was unblocked",
                "You can user Blinkstay again. Listings taken offline by the block were restored", null);
        contact(e.userId()).ifPresent(c -> emails.sendAccountUnblocked(c.username(), c.email()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(HotelManagerRequestSubmittedEvent e) {
        // every active admin, except the applicant if they happen to be an admin
        for (UserContact admin : userDirectory.findActiveAdmins()) {
            if (admin.id().equals(e.userId())) {
                continue; // an admin applying does not notify themselves
            }

            save(admin.id(), NotificationType.HOST_REQUEST_SUBMITTED, "New host request",
                    e.username() + " applied to become a host", e.requestId());
            emails.sendHostRequestSubmitted(admin.username(), admin.email(), e.username());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(HotelManagerRequestReviewedEvent e) {
        contact(e.userId()).ifPresent(c -> {
            if (e.approved()) {
                save(e.userId(), NotificationType.HOST_REQUEST_APPROVED, "Host request approved",
                        "You can now create and publish listings.", e.requestId());
                emails.sendHostRequestApproved(c.username(), c.email());
            } else {
                save(e.userId(), NotificationType.HOST_REQUEST_REJECTED, "Host request not approved",
                        "Reason: " + e.reason(), e.requestId());
                emails.sendHostRequestRejected(c.username(), c.email(), e.reason());
            }
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ListingSuspendEvent e) {
        save(e.managerId(), NotificationType.LISTING_SUSPENDED, "Listing suspended",
                "\"" + e.title() + "\" was suspended. Reason: " + e.reason(), e.listingId());
        contact(e.managerId())
                .ifPresent(c -> emails.sendListingSuspended(c.username(), c.email(), e.title(), e.reason()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ListingRestoredEvent e) {
        String where = switch (e.restoredStatus()) {
            case "PUBLISHED" -> "is live again";
            case "PAUSED" -> "is paused";
            default -> "is back in your drafts";
        };

        save(e.managerId(), NotificationType.LISTING_RESTORED, "Listing restored",
                "\"" + e.title() + "\" " + where + ".", e.listingId());
        contact(e.managerId())
                .ifPresent(c -> emails.sendListingRestored(c.username(), c.email(), e.title(), e.restoredStatus()));
    }

    private Optional<UserContact> contact(UUID userId) {
        return userDirectory.findContact(userId);
    }

    private void save(UUID userId, NotificationType type, String title, String message, UUID referenceId) {
        try {
            inApp.create(userId, type, title, message, referenceId);
        } catch (Exception ex) {
            log.error("Could not save in-app notification {} for {}: {}", type, userId, ex.getMessage());
        }
    }

    /* PAYMENT SERVICE */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(BookingConfirmedEvent e) {
        String dates = DAY.format(e.checkIn()) + " to " + DAY.format(e.checkOut());

        Optional<UserContact> guest = contact(e.guestId());
        save(e.guestId(), NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
                e.listingTitle() + ", " + dates + " (" + e.reference() + ")", e.bookingId());
        guest.ifPresent(c -> emails.sendBookingConfirmed(c.username(), c.email(), e));

        // the host sees the guest's name only, never their email or phone
        String guestName = guest.map(UserContact::username).orElse("A guest");
        save(e.hostId(), NotificationType.BOOKING_RECEIVED, "New booking",
                guestName + " booked " + e.listingTitle() + ", " + dates, e.bookingId());
        contact(e.hostId()).ifPresent(c -> emails.sendBookingReceived(c.username(), c.email(), guestName, e));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PaymentRefundedEvent e) {
        save(e.guestId(), NotificationType.PAYMENT_REFUNDED, "Payment refunded",
                "Booking " + e.bookingReference() + ": " + e.reason(), e.bookingId());
        contact(e.guestId()).ifPresent(c -> emails.sendPaymentRefunded(c.username(), c.email(), e));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(BookingCancelledEvent e) {
        String dates = DAY.format(e.checkIn()) + " to " + DAY.format(e.checkOut());
        boolean byGuest = "GUEST".equals(e.cancelledBy());
        Optional<UserContact> guest = contact(e.guestId());

        String money = "";
        if (e.wasPaid()) {
            money = e.refundAmount() != null && e.refundAmount().signum() > 0
                    ? " Refund: " + e.currency() + " " + e.refundAmount().toPlainString() + "."
                    : " No refund was due.";
        }

        // the guest hears about it, unless they cancelled their own unpaid hold
        if (e.wasPaid() || !byGuest) {
            save(e.guestId(), NotificationType.BOOKING_CANCELLED,
                    byGuest ? "Booking cancelled" : "Your booking was cancelled",
                    e.listingTitle() + ", " + dates + " (" + e.reference() + ")." + money, e.bookingId());
            guest.ifPresent(c -> emails.sendBookingCancelledToGuest(c.username(), c.email(), e));
        }

        // the host hears about paid bookings cancelled by someone else
        if (e.wasPaid() && !"HOST".equals(e.cancelledBy())) {
            String guestName = guest.map(UserContact::username).orElse("A guest");
            save(e.hostId(), NotificationType.BOOKING_CANCELLED, "Booking cancelled",
                    ("ADMIN".equals(e.cancelledBy()) ? "Support" : guestName) + " cancelled " + e.listingTitle() + ", "
                            + dates + " (" + e.reference() + ")",
                    e.bookingId());
            contact(e.hostId()).ifPresent(c -> emails.sendBookingCancelledToHost(c.username(), c.email(), guestName, e));
        }
    }
}
