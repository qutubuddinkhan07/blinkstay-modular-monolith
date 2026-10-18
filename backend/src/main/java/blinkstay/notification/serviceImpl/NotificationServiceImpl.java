package blinkstay.notification.serviceImpl;

import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.booking.PaymentRefundedEvent;
import blinkstay.notification.event.SimpleMessageEvent;
import blinkstay.notification.service.NotificationService;
import blinkstay.notification.util.EmailMessageBuilderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final EmailMessageBuilderUtil messages;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void sendOtpUserRegistration(String username, String receiverId, String subject, String otp,
                                        int validMinutes) {
        publish(receiverId, subject, messages.registrationOtpMessage(username, otp, validMinutes));
    }

    @Override
    public void sendMailUserRegistered(String username, String receiverId, String subject) {
        publish(receiverId, subject, messages.welcomeMessage(username));
    }

    @Override
    public void sendPasswordResetOtp(String username, String receiverId, String otp, int validMinutes) {
        publish(receiverId, "Your Blinkstay password reset code",
                messages.passwordResetOtpMessage(username, otp, validMinutes));
    }

    @Override
    public void sendPasswordChanged(String username, String receiverId, String subject) {
        publish(receiverId, subject, messages.passwordChangedMessage(username));
    }

    private void publish(String receiverId, String subject, String body) {
        eventPublisher.publishEvent(
                SimpleMessageEvent.builder().receiverId(receiverId).subject(subject).message(body).build());
        log.info("Email queued for {}: {}", receiverId, subject);
    }

    @Override
    public void sendAccountDeleted(String username, String receiverId) {
        publish(receiverId, "Your Blinkstay account was deleted", messages.accountDeletedMessage(username));
    }

    @Override
    public void sendAccountBlocked(String username, String receiverId, String reason) {
        publish(receiverId, "Your Blinkstay account has been blocked",
                messages.accountBlockedMessage(username, reason));
    }

    @Override
    public void sendAccountUnblocked(String username, String receiverId) {
        publish(receiverId, "Your Blinkstay account has been unblocked", messages.accountUnblockedMessage(username));
    }

    @Override
    public void sendHostRequestSubmitted(String adminName, String adminEmail, String applicantName) {
        publish(adminEmail, "New host request to review",
                messages.hostRequestSubmittedAdminMessage(adminName, applicantName));
    }

    @Override
    public void sendHostRequestApproved(String username, String receiverId) {
        publish(receiverId, "Your host request was approved", messages.hostRequestApprovedMessage(username));
    }

    @Override
    public void sendHostRequestRejected(String username, String receiverId, String reason) {
        publish(receiverId, "Your host request was not approved",
                messages.hostRequestRejectedMessage(receiverId, reason));
    }

    @Override
    public void sendListingSuspended(String username, String receiverId, String title, String reason) {
        publish(receiverId, "Your listing was suspended", messages.listingSuspendedMessage(username, title, reason));
    }

    @Override
    public void sendListingRestored(String username, String receiverId, String title, String restoredStatus) {
        publish(receiverId, "Your listing was restored",
                messages.listingRestoredMessage(username, title, restoredStatus));
    }

    @Override
    public void sendBookingConfirmed(String username, String receiverId, BookingConfirmedEvent event) {
        publish(receiverId, "Your Blinkstay booking is confirmed", messages.bookingConfirmedMessage(username, event));
    }

    @Override
    public void sendBookingReceived(String hostName, String hostEmail, String guestName, BookingConfirmedEvent event) {
        publish(hostEmail, "New booking received", messages.bookingReceivedMessage(hostName, guestName, event));
    }

    @Override
    public void sendPaymentRefunded(String username, String receiverId, PaymentRefundedEvent event) {
        publish(receiverId, "Your payment was refunded", messages.paymentRefundedMessage(username, event));
    }

}
