package blinkstay.auth.scheduler;

import blinkstay.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeletedAccountPurgeJob {
    public static final int RETENTION_DAYS = 30;

    private final UserRepository userRepository;
    private final DeletedAccountPurger purger;

    // every night at 03:30, after the midnight token cleanup
    // @Scheduled(cron = "0 * * * * *") // for immediate running
    @Scheduled(cron = "0 30 3 * * *")
    public void purgeExpiredAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<UUID> ids = userRepository.findIdsToPurge(cutoff);
        int done = 0;

        for (UUID id : ids) {
            try {
                if (purger.purge(id, cutoff)) {
                    done++;
                }
            } catch (Exception e) {
                // one bad account must not stop the others
                log.error("Purge failed for user {}", id, e);
            }
        }
        if (!ids.isEmpty()) {
            log.info("Deleted-account purge: {} of {} accounts purged", done, ids.size());
        }
    }
}