package com.blinkstay.auth.runners;

import com.blinkstay.auth.repository.UserRepository;
import com.blinkstay.common.cloudinary.CloudinaryFolders;
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
 * DEV ONLY: if the database has NO users, empty this environment's profile-picture
 * folder (dev/blinkstay_user_profiles). It can never run in production, and it can
 * never see another environment's folder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "auth.cloudinary-cleanup.enabled", havingValue = "true", matchIfMissing = false)
public class UserCloudinaryCleanup implements CommandLineRunner {
    private final UserRepository userRepo;
    private final Cloudinary cloudinary;
    private final CloudinaryFolders folders;

    @Override
    public void run(String... args) {
        String folder = folders.userProfiles();
        if (userRepo.count() > 0) {
            log.info("Users exist: Cloudinary folder '{}' left untouched", folder);
            return;
        }
        log.warn("No users in the database: deleting all images in Cloudinary folder '{}'", folder);
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
            more = Boolean.TRUE.equals(res.get("partial"));
            rounds++;
        } while (more && rounds < 100);
        return deleted;
    }
}