package blinkstay.notification.serviceImpl;

import blinkstay.notification.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    /**
     * The SMTP login. Used as the From address when app.mail.from is empty.
     */
    @Value("${spring.mail.username:noreply@blinkstay.local}")
    private String smtpUsername;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.mail.from-name:BlinkStay}")
    private String fromName;

    @Value("${app.support.email:}")
    private String supportEmail;

    /**
     * Console mode only: if set, each HTML email is also saved here so you can open it in a browser.
     */
    @Value("${app.mail.preview-dir:}")
    private String previewDir;

    @Override
    public boolean sentEmail(String receiverId, String subject, String text, String html) {
        // a subject must be one line; also blocks header injection through user-supplied titles
        String cleanSubject = subject == null ? "" : subject.replaceAll("[\\r\\n]+", " ").trim();

        if (!enabled) {
            log.info("MAIL (console mode, nothing sent)\n  to: {}\n  subject: {}\n  ----\n{}\n  ----", receiverId,
                    cleanSubject, text);
            savePreview(cleanSubject, html);
            return true;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.error("app.mail.enabled=true but no mail server is configured (spring.mail.host)");
            return false;
        }

        try {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String from = (fromAddress == null || fromAddress.isBlank()) ? smtpUsername : fromAddress.trim();
            helper.setFrom(from, fromName); // shows as "BlinkStay <you@domain>" in the inbox
            helper.setTo(receiverId);
            if (supportEmail != null && !supportEmail.isBlank()) {
                helper.setReplyTo(supportEmail.trim()); // replies go to a real inbox, not the sender account
            }
            helper.setSubject(cleanSubject);

            if (html != null && !html.isBlank()) {
                helper.setText(text == null ? "" : text, html); // text + HTML parts: better for spam filters
            } else {
                helper.setText(text == null ? "" : text, false);
            }

            sender.send(mimeMessage);
            return true;
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", receiverId, e.getMessage());
            return false;
        }
    }

    private void savePreview(String subject, String html) {
        if (html == null || previewDir == null || previewDir.isBlank()) {
            return;
        }
        try {
            Path dir = Path.of(previewDir.trim());
            Files.createDirectories(dir);
            String slug = subject.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            if (slug.length() > 60) {
                slug = slug.substring(0, 60);
            }
            Path file = dir.resolve(LocalDateTime.now().format(FILE_TIME) + "-" + slug + ".html");
            Files.writeString(file, html, StandardCharsets.UTF_8);
            log.info("MAIL preview saved: {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not save mail preview: {}", e.getMessage());
        }
    }
}