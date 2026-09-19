package blinkstay.notification.service;

public interface NotificationService {
    void sendOtpUserRegistration(String username, String receiverId, String subject, String otp);

    void sendMailUserRegistered(String username, String receiverId, String subject);
}
