package blinkstay.auth.service;

public interface PasswordResetService {
	void requestReset(String username, String email);

	void resetPassword(String email, String otp, String newPassword);
}
