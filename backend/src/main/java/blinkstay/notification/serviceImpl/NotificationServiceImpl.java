package blinkstay.notification.serviceImpl;

import blinkstay.notification.event.SimpleMessageEvent;
import blinkstay.notification.service.NotificationService;
import blinkstay.notification.util.EmailMessageBuilderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final EmailMessageBuilderUtil emailMessageBuilderUtil;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void sendOtpUserRegistration(String username, String receiverId, String subject, String otp) {
        log.info("sendOtpUserRegistration() called for {}", receiverId);

        String otpMessage = emailMessageBuilderUtil.otpMessageBuilder(username, receiverId, otp);

        SimpleMessageEvent emailEvent = SimpleMessageEvent.builder().receiverId(receiverId)
                .message(otpMessage).subject(subject).build();

        eventPublisher.publishEvent(emailEvent);
        log.info("Send OTP event published for {}", receiverId);
    }

    @Override
    public void sendMailUserRegistered(String username, String receiverId, String subject) {
        String message = emailMessageBuilderUtil.userRegisteredMessageBuilder(receiverId);
        SimpleMessageEvent event = SimpleMessageEvent.builder()
                .receiverId(receiverId)
                .subject(subject)
                .message(message)
                .build();

        eventPublisher.publishEvent(event);
    }
}
