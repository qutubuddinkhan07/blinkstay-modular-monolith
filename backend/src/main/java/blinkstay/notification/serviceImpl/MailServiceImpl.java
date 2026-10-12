package blinkstay.notification.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import blinkstay.notification.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailServiceImpl implements MailService {
	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String senderId;

	@Override
	public boolean sentEmail(String receiverId, String htmlBody, String subject) {
//		log.info("DEBUG senderId: {}", senderId);
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			helper.setFrom(senderId);
			helper.setTo(receiverId);
			helper.setSubject(subject);
			helper.setText(htmlBody, true); // true = HTML content
			mailSender.send(mimeMessage);

			return true;
		} catch (MessagingException | MailException mailException) {
			throw new MailSendException("Failed to send email", mailException);
		}
	}
}
