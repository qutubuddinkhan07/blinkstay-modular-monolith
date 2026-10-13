package blinkstay.notification.util;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy(true)
public class EmailMessageBuilderUtil {
	private static final String FOOTER = "\n\nThis is an automated message from BlinkStay. Please do not reply.";

	public String registrationOtpMessage(String username, String otp, int validMinutes) {
		return "Hi " + username + ",\n\nYour BlinkStay verification code is: " + otp + "\n\nIt is valid for "
				+ validMinutes + " minutes. If you did not try to sign up, you can ignore this email." + FOOTER;
	}

	public String passwordResetOtpMessage(String username, String otp, int validMinutes) {
		return "Hi " + username + ",\n\nYour BlinkStay password reset code is: " + otp + "\n\nIt is valid for "
				+ validMinutes + " minutes. If you did not ask for a reset, ignore this email: your password has "
				+ "not been changed." + FOOTER;
	}

	public String welcomeMessage(String username) {
		return "Hi " + username + ",\n\nWelcome to BlinkStay. Your account is ready, and you can log in now." + FOOTER;
	}

	public String passwordChangedMessage(String username) {
		return "Hi " + username + ",\n\nYour BlinkStay password was just changed. If this was you, no action is "
				+ "needed. If it was not, reset your password straight away and contact support." + FOOTER;
	}
}