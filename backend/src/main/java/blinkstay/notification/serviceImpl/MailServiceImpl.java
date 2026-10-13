package blinkstay.notification.serviceImpl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import blinkstay.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
	private final ObjectProvider<JavaMailSender> mailSenderProvider;

	@Value("${app.mail.enable:false}")
	private boolean enabled;

	@Value("${spring.mail.username:noreply@blinkstay.local}")
	private String senderId;

	@Override
	public boolean sentEmail(String receiverId, String message, String subject) {
		if (!enabled) {
			log.info("MAIL (console mode, nothing sent)\n  to: {}\n  subject: {}\n  ----\n{}\n  ----", receiverId,
					subject, message);
			return true;
		}

		JavaMailSender sender = mailSenderProvider.getIfAvailable();
		if (sender == null) {
			log.error("app.mail.enabled=true but no mail server is configured (spring.mail.host)");
		}

		try {
			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setTo(receiverId);
			mail.setFrom(senderId);
			mail.setSubject(subject);
			mail.setText(message);
			sender.send(mail);
			return true;
		} catch (MailException e) {
			// TODO: handle exception
			log.error("Failed to send email to {}: {}", receiverId, e.getMessage());
			return false;
		}
	}
}
