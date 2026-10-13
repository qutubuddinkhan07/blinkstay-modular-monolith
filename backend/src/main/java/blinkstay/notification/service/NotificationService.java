package blinkstay.notification.service;

public interface NotificationService {
	void sendOtpUserRegistration(String username, String receiverId, String subject, String otp, int validMinutes);

	void sendMailUserRegistered(String username, String receiverId, String subject);

	void sendPasswordResetOtp(String username, String receiverId, String otp, int validMinutes);

	void sendPasswordChanged(String username, String receiverId, String subject);
}
