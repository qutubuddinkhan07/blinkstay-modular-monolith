package com.blinkstay.listing.scheduler;

import com.blinkstay.listing.service.CloudinaryOrphanCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly orphan cleanup. OFF unless listing.cloudinary-orphan-cleanup.enabled=true.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "listing.cloudinary-orphan-cleanup.enabled", havingValue = "true", matchIfMissing = false)
public class CloudinaryCleanupScheduler {
    private final CloudinaryOrphanCleanupService orphanCleanupService;

    // every day at 00:00
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleOrphanCleanupAtMidnight() {
        log.info("Starting scheduled Cloudinary orphan image cleanup...");
        orphanCleanupService.cleanupOrphanListingImages();
    }
}