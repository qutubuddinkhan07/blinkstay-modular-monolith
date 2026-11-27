package com.blinkstay.listing.runners;

import com.blinkstay.common.cloudinary.CloudinaryFolders;
import com.blinkstay.listing.repository.ListingImageRepository;
import com.blinkstay.listing.repository.ListingRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DEV ONLY: if the listing tables are empty, empty this environment's listing-image folder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "listing.cloudinary-cleanup.enabled", havingValue = "true", matchIfMissing = false)
public class ListingCloudinaryCleanupRunner implements CommandLineRunner {
    private final Cloudinary cloudinary;
    private final ListingRepository listingRepo;
    private final ListingImageRepository listingImageRepo;
    private final CloudinaryFolders folders;

    @Override
    public void run(String... args) {
        String folder = folders.listings();
        long listings = listingRepo.count();
        long images = listingImageRepo.count();
        if (listings > 0 || images > 0) {
            log.info("Listing data exists (listings={}, images={}): Cloudinary folder '{}' left untouched",
                    listings, images, folder);
            return;
        }
        log.warn("Listing tables are empty: deleting all images in Cloudinary folder '{}'", folder);
        try {
            log.warn("Cloudinary folder '{}' cleaned: {} images deleted", folder, deleteAllInFolder(folder));
        } catch (Exception e) {
            log.error("Failed to clean Cloudinary folder '{}'", folder, e); // never block startup
        }
    }

    @SuppressWarnings("unchecked")
    private int deleteAllInFolder(String folder) throws Exception {
        String prefix = folder + "/";
        int deleted = 0;
        int rounds = 0;
        boolean more;
        do {
            Map<String, Object> res = cloudinary.api().deleteResourcesByPrefix(prefix,
                    ObjectUtils.asMap("resource_type", "image", "type", "upload"));
            if (res.get("deleted") instanceof Map<?, ?> map) {
                deleted += (int) map.values().stream().filter("deleted"::equals).count();
            }
            more = Boolean.TRUE.equals(res.get("partial")); // up to 1000 per call
            rounds++;
        } while (more && rounds < 100);
        return deleted;
    }
}