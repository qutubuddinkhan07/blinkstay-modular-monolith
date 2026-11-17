package blinkstay.listing.serviceImpl;

import blinkstay.common.cloudinary.CloudinaryFolders;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.listing.service.CloudinaryOrphanCleanupService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deletes listing images whose listing no longer exists, with safety rails:
 * only this environment's folder, only files older than min-age-hours, a dry run by
 * default, and a hard stop when "too many" orphans suggest the wrong database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryOrphanCleanupServiceImpl implements CloudinaryOrphanCleanupService {
    private static final Pattern UUID_PATTERN = Pattern
            .compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final Cloudinary cloudinary;
    private final ListingRepository listingRepository;
    private final CloudinaryFolders folders;

    @Value("${listing.cloudinary-orphan-cleanup.dry-run:true}")
    private boolean dryRun;

    @Value("${listing.cloudinary-orphan-cleanup.min-age-hours:24}")
    private long minAgeHours;

    @Value("${listing.cloudinary-orphan-cleanup.max-deletes:20}")
    private int maxDeletes;

    @Override
    @SuppressWarnings("unchecked")
    public void cleanupOrphanListingImages() {
        String prefix = folders.listings() + "/";
        Instant cutoff = Instant.now().minus(Duration.ofHours(minAgeHours));
        List<String> orphans = new ArrayList<>();
        int scanned = 0;

        try {
            String cursor = null;
            do {
                Map<String, Object> params = ObjectUtils.asMap("type", "upload", "prefix", prefix,
                        "max_results", 500);
                if (cursor != null) {
                    params.put("next_cursor", cursor);
                }
                Map<String, Object> res = cloudinary.api().resources(params);
                List<Map<String, Object>> resources = (List<Map<String, Object>>) res.get("resources");
                if (resources != null) {
                    for (Map<String, Object> r : resources) {
                        scanned++;
                        String publicId = (String) r.get("public_id");
                        if (isOrphan(publicId, r.get("created_at"), cutoff)) {
                            orphans.add(publicId);
                        }
                    }
                }
                cursor = (String) res.get("next_cursor");
            } while (cursor != null);
        } catch (Exception e) {
            log.error("Cloudinary orphan scan failed for '{}'", prefix, e);
            return;
        }

        if (orphans.isEmpty()) {
            log.info("Orphan cleanup: scanned {} images in '{}', none orphaned", scanned, prefix);
            return;
        }
        if (orphans.size() > maxDeletes) {
            log.warn("SAFETY STOP: {} orphan images found in '{}' (limit {}). Nothing deleted. "
                            + "Check that the app is connected to the right database.", orphans.size(), prefix,
                    maxDeletes);
            return;
        }
        if (dryRun) {
            log.warn("DRY RUN: would delete {} orphan images in '{}': {}", orphans.size(), prefix, orphans);
            return;
        }
        for (String publicId : orphans) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.warn("Deleted orphan image {}", publicId);
            } catch (Exception e) {
                log.error("Could not delete orphan image {}", publicId, e);
            }
        }
    }

    private boolean isOrphan(String publicId, Object createdAt, Instant cutoff) {
        if (publicId == null || createdAt == null) {
            return false;
        }
        try {
            if (Instant.parse(createdAt.toString()).isAfter(cutoff)) {
                return false; // too new: an upload may still be in progress
            }
        } catch (DateTimeParseException e) {
            return false;
        }
        Matcher m = UUID_PATTERN.matcher(publicId);
        if (!m.find()) {
            return false;
        }
        try {
            return !listingRepository.existsById(UUID.fromString(m.group()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}