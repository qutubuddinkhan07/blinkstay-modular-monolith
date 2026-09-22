package blinkstay.listing.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import blinkstay.listing.service.CloudinaryOrphanCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryCleanupScheduler {

	private final CloudinaryOrphanCleanupService orphanCleanupService;

	/**
	 * Cron expression format: "second minute hour day-of-month month day-of-week"
	 * 
	 * "0 0 0 * * ?" = Runs every day at 00:00:00 (Midnight)
	 */
	@Scheduled(cron = "0 0 0 * * ?")
	public void scheduleOrphanCleanupAtMidnight() {
		log.info("Starting scheduled Cloudinary orphan image cleanup task...");
		orphanCleanupService.cleanupOrphanListingImages();
	}

	/**
	 * Optional Alternative: Run every 12 hours (fixedRate in milliseconds) 12 hours
	 * = 12 * 60 * 60 * 1000 = 43,200,000 ms
	 */
	// @Scheduled(fixedRate = 43200000)
	// public void scheduleOrphanCleanupEvery12Hours() {
	// orphanCleanupService.cleanupOrphanListingImages();
	// }
}
