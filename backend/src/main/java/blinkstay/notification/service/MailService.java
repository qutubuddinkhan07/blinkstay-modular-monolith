package blinkstay.notification.service;

import org.springframework.stereotype.Service;

public interface MailService {
    public boolean sentEmail(String receiverId, String message);
}
