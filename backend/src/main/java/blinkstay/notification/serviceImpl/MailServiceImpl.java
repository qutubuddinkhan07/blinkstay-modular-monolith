package blinkstay.notification.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import blinkstay.notification.service.MailService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailServiceImpl implements MailService {
	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String senderId;

	@Override
	public boolean sentEmail(String receiverId, String message, String subject) {
//		log.info("DEBUG senderId: {}", senderId);
		try {
			SimpleMailMessage mailMessage = new SimpleMailMessage();
			mailMessage.setTo(receiverId);
			mailMessage.setFrom(senderId);
			mailMessage.setSubject(subject);
			mailMessage.setText(message);
			mailSender.send(mailMessage);
			return true;
		} catch (MailException mailException) {
			throw new MailSendException("Failed to send email", mailException);
		}
	}
}
