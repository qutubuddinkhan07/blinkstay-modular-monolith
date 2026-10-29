package blinkstay.payment.timer;

import blinkstay.payment.serviceImpl.RefundProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundTimers {
    private final RefundProcessor processor;

    @Scheduled(fixedDelay = 30_000, initialDelay = 45_000)
    public void retryDueRefunds() {
        int n = processor.runDue();
        if (n > 0) {
            log.info("Tried {} pending refunds", n);
        }
    }
}
