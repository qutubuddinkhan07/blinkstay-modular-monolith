package blinkstay.notification;

import blinkstay.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;

//@Component
@Lazy(value = true)
@RequiredArgsConstructor
@Slf4j
public class MailTestRunner implements CommandLineRunner {
    private final MailService mailService;

    @Override
    public void run(String... args) throws Exception {
//        boolean sent = mailService.sentEmail("qutubuddink267@gmail.com", "Hinata is back!!");
//        if(sent)
//            log.info("Mail sent");
    }
}
