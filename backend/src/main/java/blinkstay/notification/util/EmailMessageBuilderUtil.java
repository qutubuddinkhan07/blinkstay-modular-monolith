package blinkstay.notification.util;

import org.springframework.stereotype.Component;

@Component
public class EmailMessageBuilderUtil {
    public String otpMessageBuilder(String username, String email, String otp) {
        StringBuilder message = new StringBuilder();
        message.append("Dear " + username + " : " + email + ",\n");
        message.append("Thank you for your interest in Blinkstay.\n");
        message.append("For your new account registration the 6 digit otp is here: " + otp);
        message.append("\nNote: OTP valid only for 5 minutes.");
        return message.toString();
    }

    public String userRegisteredMessageBuilder(String username) {
        StringBuilder message = new StringBuilder();
        message.append("Dear " + username + ",\n");
        message.append("Thank you for your interest for our services, Blinkstay.\n");
        message.append("You have been registered as user.\n");
        message.append("Note: This is a system generated email. Do not reply.");
        return message.toString();
    }
}
