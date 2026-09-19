package blinkstay.notification;

import blinkstay.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

//@Component
@Lazy(value = true)
@RequiredArgsConstructor
public class MailTestRunner implements CommandLineRunner {
    private final MailService mailService;

    @Override
    public void run(String... args) throws Exception {
        mailService.sentEmail("qutubuddink267@gmail.com", "Hinata is back!!");
    }
}
