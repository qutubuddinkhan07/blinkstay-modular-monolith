package blinkstay.notification.util;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy(value = true)
public class EmailMessageBuilderUtil {

	// Brand tokens pulled from your Logo component
	private static final String BRAND_PRIMARY = "#710019"; // light-mode primary (the red B)
	private static final String BRAND_TEXT = "#002642"; // light-mode text (Blinkstay wordmark)
	private static final String MUTED_COLOR = "#717171";
	private static final String BG_COLOR = "#F7F7F7";
	private static final String CARD_BG = "#FFFFFF";

	/**
	 * Branded header with the exact "B" tile + Blinkstay wordmark from the website
	 * logo.
	 */
	private String buildLogoHeader() {
		return "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='margin:0 auto;'>" + "<tr>"
		// The "B" tile
				+ "<td style='width:36px; height:36px; background:" + BRAND_PRIMARY
				+ "; border-radius:12px; text-align:center; vertical-align:middle;'>"
				+ "<span style='display:inline-block; font-size:18px; font-weight:600; color:#F8F9FC; font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; line-height:36px;'>B</span>"
				+ "</td>"
				// Spacer between icon and wordmark (matches gap-2.5 = 10px)
				+ "<td style='width:10px;'>&nbsp;</td>"
				// Blinkstay wordmark
				+ "<td style='vertical-align:middle;'>" + "<span style='font-size:18px; font-weight:600; color:"
				+ BRAND_TEXT
				+ "; letter-spacing:-0.4px; font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif;'>Blinkstay</span>"
				+ "</td>" + "</tr></table>";
	}

	/**
	 * Wraps any content in the branded Blinkstay email layout.
	 */
	private String wrapTemplate(String preheader, String contentHtml) {
		return "<!DOCTYPE html>" + "<html><head><meta charset='UTF-8'>"
				+ "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
				+ "<title>Blinkstay</title></head>" + "<body style='margin:0; padding:0; background-color:" + BG_COLOR
				+ "; font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif;'>"
				// Hidden preview text (shown next to subject line in inbox)
				+ "<div style='display:none; max-height:0; overflow:hidden; opacity:0;'>" + preheader + "</div>"
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background-color:"
				+ BG_COLOR + "; padding: 40px 0;'>" + "<tr><td align='center'>"
				+ "<table role='presentation' width='600' cellpadding='0' cellspacing='0' style='max-width:600px; width:100%; background:"
				+ CARD_BG + "; border-radius:12px; overflow:hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06);'>"
				// Header with the real logo
				+ "<tr><td style='padding: 32px 40px 8px 40px; text-align:center;'>" + buildLogoHeader() + "</td></tr>"
				// Body
				+ "<tr><td style='padding: 16px 40px 32px 40px; color:" + BRAND_TEXT
				+ "; font-size:16px; line-height:1.6;'>" + contentHtml + "</td></tr>"
				// Footer
				+ "<tr><td style='padding: 24px 40px; background:" + BG_COLOR + "; text-align:center; color:"
				+ MUTED_COLOR + "; font-size:13px; line-height:1.5;'>"
				+ "This is a system-generated email. Please do not reply.<br>" + "&copy; "
				+ java.time.Year.now().getValue() + " Blinkstay. All rights reserved." + "</td></tr>"
				+ "</table></td></tr></table></body></html>";
	}

	/**
	 * OTP email — large centered code block.
	 */
	public String otpMessageBuilder(String username, String email, String otp) {
		String content = "<p style='margin:0 0 16px 0;'>Hi <strong>" + username + "</strong>,</p>"
				+ "<p style='margin:0 0 24px 0; color:" + MUTED_COLOR
				+ ";'>Thank you for your interest in Blinkstay. Use the code below to verify your account.</p>"
				+ "<div style='text-align:center; margin: 32px 0;'>" + "<div style='display:inline-block; background:"
				+ BG_COLOR + "; border-radius:10px; padding: 20px 40px;'>"
				+ "<span style='font-size:32px; font-weight:700; letter-spacing:8px; color:" + BRAND_TEXT + ";'>" + otp
				+ "</span>" + "</div></div>" + "<p style='margin:0; color:" + MUTED_COLOR
				+ "; font-size:14px;'>This code is valid for <strong>5 minutes</strong>. If you didn't request this, you can safely ignore this email.</p>";

		return wrapTemplate("Your Blinkstay verification code is " + otp, content);
	}

	/**
	 * Welcome email — friendly with a branded CTA button.
	 */
	public String userRegisteredMessageBuilder(String username) {
		String content = "<p style='margin:0 0 16px 0;'>Hi <strong>" + username + "</strong>,</p>"
				+ "<p style='margin:0 0 24px 0; color:" + MUTED_COLOR
				+ ";'>Welcome to Blinkstay! Your account has been created successfully. We're excited to have you with us.</p>"
				+ "<div style='text-align:center; margin: 32px 0;'>"
				+ "<a href='https://blinkstay.com' style='display:inline-block; background:" + BRAND_PRIMARY
				+ "; color:#F8F9FC; text-decoration:none; font-weight:600; padding: 14px 32px; border-radius:10px; font-size:16px;'>Explore Blinkstay</a>"
				+ "</div>" + "<p style='margin:0; color:" + MUTED_COLOR
				+ "; font-size:14px;'>If you have any questions, just reply to this email — we're happy to help.</p>";

		return wrapTemplate("Welcome to Blinkstay, " + username + "!", content);
	}
}