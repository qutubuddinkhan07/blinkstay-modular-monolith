package blinkstay.earnings.timer;

import blinkstay.earnings.serviceImpl.EarningsLedger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EarningsTimers {
    private final EarningsLedger ledger;

    // hourly by default: the property lets you shorten it while testing
    @Scheduled(fixedDelayString = "${app.earnings.ear-interval-ms:3600000}", initialDelay = 90_000)
    public void markEarned() {
        int n = ledger.markEarned();
        if (n > 0) {
            log.info("Marked {} earnings rows as earned", n);
        }
    }
}
