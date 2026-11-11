package blinkstay.notification.serviceImpl;

import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.booking.PaymentRefundedEvent;
import blinkstay.common.payment.BookingCancelledEvent;
import blinkstay.notification.event.SimpleMessageEvent;
import blinkstay.notification.service.NotificationService;
import blinkstay.notification.util.EmailContent;
import blinkstay.notification.util.EmailMessageBuilderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/*
 * Subjects now come from EmailMessageBuilderUtil (one consistent style for every
 * email). The "subject" parameters some methods still receive are ignored; they
 * are kept only so NotificationService and its callers don't have to change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final EmailMessageBuilderUtil messages;

    private final ApplicationEventPublisher eventPublisher;

    private void publish(String receiverId, EmailContent content) {
        eventPublisher.publishEvent(
                SimpleMessageEvent.builder().receiverId(receiverId).content(content).build());
        log.info("Email queued for {}: {}", receiverId, content.subject());
    }

    /* ---------------- account ---------------- */

    @Override
    public void sendOtpUserRegistration(String username, String receiverId, String subject, String otp,
                                        int validMinutes) {
        publish(receiverId, messages.registrationOtpMessage(username, otp, validMinutes));
    }

    @Override
    public void sendMailUserRegistered(String username, String receiverId, String subject) {
        publish(receiverId, messages.welcomeMessage(username));
    }

    @Override
    public void sendPasswordResetOtp(String username, String receiverId, String otp, int validMinutes) {
        publish(receiverId, messages.passwordResetOtpMessage(username, otp, validMinutes));
    }

    @Override
    public void sendPasswordChanged(String username, String receiverId, String subject) {
        publish(receiverId, messages.passwordChangedMessage(username));
    }

    @Override
    public void sendAccountDeleted(String username, String receiverId) {
        publish(receiverId, messages.accountDeletedMessage(username));
    }

    /* ---------------- moderation ---------------- */

    @Override
    public void sendAccountBlocked(String username, String receiverId, String reason) {
        publish(receiverId, messages.accountBlockedMessage(username, reason));
    }

    @Override
    public void sendAccountUnblocked(String username, String receiverId) {
        publish(receiverId, messages.accountUnblockedMessage(username));
    }

    @Override
    public void sendHostRequestSubmitted(String adminName, String adminEmail, String applicantName) {
        publish(adminEmail, messages.hostRequestSubmittedAdminMessage(adminName, applicantName));
    }

    @Override
    public void sendHostRequestApproved(String username, String receiverId) {
        publish(receiverId, messages.hostRequestApprovedMessage(username));
    }

    @Override
    public void sendHostRequestRejected(String username, String receiverId, String reason) {
        // fixed: was passing receiverId (the email) instead of username
        publish(receiverId, messages.hostRequestRejectedMessage(username, reason));
    }

    @Override
    public void sendListingSuspended(String username, String receiverId, String title, String reason) {
        publish(receiverId, messages.listingSuspendedMessage(username, title, reason));
    }

    @Override
    public void sendListingRestored(String username, String receiverId, String title, String restoredStatus) {
        publish(receiverId, messages.listingRestoredMessage(username, title, restoredStatus));
    }

    /* ---------------- bookings ---------------- */

    @Override
    public void sendBookingConfirmed(String username, String receiverId, BookingConfirmedEvent event) {
        publish(receiverId, messages.bookingConfirmedMessage(username, event));
    }

    @Override
    public void sendBookingReceived(String hostName, String hostEmail, String guestName, BookingConfirmedEvent event) {
        publish(hostEmail, messages.bookingReceivedMessage(hostName, guestName, event));
    }

    @Override
    public void sendPaymentRefunded(String username, String receiverId, PaymentRefundedEvent event) {
        publish(receiverId, messages.paymentRefundedMessage(username, event));
    }

    @Override
    public void sendBookingCancelledToGuest(String username, String receiverId, BookingCancelledEvent event) {
        publish(receiverId, messages.bookingCancelledGuestMessage(username, event));
    }

    @Override
    public void sendBookingCancelledToHost(String hostName, String hostMail, String guestName, BookingCancelledEvent event) {
        publish(hostMail, messages.bookingCancelledHostMessage(hostName, guestName, event));
    }
}