package blinkstay.notification.serviceImpl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import blinkstay.notification.event.SimpleMessageEvent;
import blinkstay.notification.service.NotificationService;
import blinkstay.notification.util.EmailMessageBuilderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

}
