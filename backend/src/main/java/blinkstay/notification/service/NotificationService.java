package blinkstay.notification.service;

import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.booking.PaymentRefundedEvent;
import blinkstay.common.payment.BookingCancelledEvent;

public interface NotificationService {
    void sendOtpUserRegistration(String username, String receiverId, String subject, String otp, int validMinutes);

    void sendMailUserRegistered(String username, String receiverId, String subject);

    void sendPasswordResetOtp(String username, String receiverId, String otp, int validMinutes);

    void sendPasswordChanged(String username, String receiverId, String subject);

    void sendAccountDeleted(String username, String receiverId);

    /*
     * Event email - blocked, unblocked, host request submitted (to admins), host
     * request approved or rejected, and listing suspended or restored
     */
    void sendAccountBlocked(String username, String receiverId, String reason);

    void sendAccountUnblocked(String username, String receiverId);

    void sendHostRequestSubmitted(String adminName, String adminEmail, String applicantName);

    void sendHostRequestApproved(String username, String receiverId);

    void sendHostRequestRejected(String username, String receiverId, String reason);

    void sendListingSuspended(String username, String receiverId, String title, String reason);

    void sendListingRestored(String username, String receiverId, String title, String restoredStatus);

    /* PAYMENT SERVICE*/
    void sendBookingConfirmed(String username, String receiverId, BookingConfirmedEvent event);

    void sendBookingReceived(String hostName, String hostEmail, String guestName, BookingConfirmedEvent event);

    void sendPaymentRefunded(String username, String receiverId, PaymentRefundedEvent event);

    void sendBookingCancelledToGuest(String username, String receiverId, BookingCancelledEvent event);

    void sendBookingCancelledToHost(String hostName, String hostMail, String guestName, BookingCancelledEvent event);
}
