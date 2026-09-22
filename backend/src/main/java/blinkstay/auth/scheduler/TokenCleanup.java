package blinkstay.auth.scheduler;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import blinkstay.auth.repository.BlockedTokenRepositry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TokenCleanup {
	@Autowired
	private BlockedTokenRepositry blockedTokenRepo;

	@Transactional
	// @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
	@Scheduled(cron = "0 0 0 * * *")
	public void cleanExpiredTokens() {
		blockedTokenRepo.deleteByExpiresAtBefore(LocalDateTime.now());
		log.info("Expired tokens deleted");
	}
}
