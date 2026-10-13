package blinkstay.notification.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy(true)
public class EmailMessageBuilderUtil {
	@Value("${app.support.email:support@blinkstay.local}")
	private String supportEmail;

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

	public String accountDeletedMessage(String username) {
		return "Hi " + username + ",\n\nYour BlinkStay account has been deleted and your listings have been taken "
				+ "offline. If you did not do this, contact support immediately." + FOOTER;
	}

	/*
	 * Event email - blocked, unblocked, host request submitted (to admins), host
	 * request approved or rejected, and listing suspended or restored
	 */

	public String accountBlockedMessage(String username, String reason) {
		return "Hi " + username + ",\n\nYour BlinkStay account has been blocked by an administrator.\n\nReason: "
				+ reason + "\n\nWhile your account is blocked you cannot log in, and any listings you own are taken "
				+ "offline. If you think this is a mistake, contact support at " + supportEmail + "." + FOOTER;
	}

	public String accountUnblockedMessage(String username) {
		return "Hi " + username + ",\n\nYour BlinkStay account has been unblocked and you can log in again. Listings "
				+ "that were taken offline because of the block have been restored to their previous status." + FOOTER;
	}

	public String hostRequestSubmittedAdminMessage(String adminName, String applicantName) {
		return "Hi " + adminName + ",\n\n" + applicantName + " has applied to become a host. The request is waiting "
				+ "in the host requests queue." + FOOTER;
	}

	public String hostRequestApprovedMessage(String username) {
		return "Hi " + username + ",\n\nYour request to become a host has been approved. You can now create and "
				+ "publish listings." + FOOTER;
	}

	public String hostRequestRejectedMessage(String username, String reason) {
		return "Hi " + username + ",\n\nYour request to become a host was not approved.\n\nReason: " + reason
				+ "\n\nYou can apply again with updated details." + FOOTER;
	}

	public String listingSuspendedMessage(String username, String title, String reason) {
		return "Hi " + username + ",\n\nYour listing \"" + title + "\" has been suspended by an administrator and is "
				+ "no longer visible.\n\nReason: " + reason + "\n\nContact support at " + supportEmail
				+ " if you would like it reviewed." + FOOTER;
	}

	public String listingRestoredMessage(String username, String title, String restoredStatus) {
		String where = switch (restoredStatus) {
		case "PUBLISHED" -> "It is live again.";
		case "PAUSED" -> "It is paused, so you can resume it when you are ready.";
		default -> "It is back in your drafts.";
		};
		return "Hi " + username + ",\n\nThe suspension on your listing \"" + title + "\" has been lifted. " + where
				+ FOOTER;
	}
}