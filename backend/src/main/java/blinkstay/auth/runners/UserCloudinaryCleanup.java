package blinkstay.auth.runners;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import blinkstay.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * On startup: if there are NO users in the database, delete everything in the
 * Cloudinary profile-pictures folder. If any user exists, nothing is touched.
 * Runs before UserDumping (@Order(1)), so the seeded users are created
 * afterwards.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "auth.cloudinary-cleanup.enabled", havingValue = "true", matchIfMissing = false)
public class UserCloudinaryCleanup implements CommandLineRunner {
	private final UserRepository userRepo;

	private final Cloudinary cloudinary;

	@Value("${auth.cloudinary-cleanup.folder:blinkstay_user_profiles}")
	private String folder;

	@Override
	public void run(String... args) throws Exception {
		if (userRepo.count() > 0) {
			log.info("Users exist: Cloudinary folder '{}' left untouched", folder);
			return;
		}

		log.warn("No users in the database: deleting all images in Cloudinary folder '{}'");

		try {
			int deleted = deleteAllInFolder(folder);
			log.warn("Cloudinary folder '{}' cleaned: {} images deleted", folder, deleted);
		} catch (Exception e) {
			// Never block the application startup because of a cleanup failure
			log.error("Failed to clean Cloudinary folder '{}'", folder, e);
		}
	}

	@SuppressWarnings("unchecked")
	private int deleteAllInFolder(String folder) throws Exception {
		String prefix = folder.endsWith("/") ? folder : folder + "/";
		int deleted = 0;
		int rounds = 0;
		boolean more;

		do {
			Map<String, Object> res = cloudinary.api().deleteResourcesByPrefix(prefix,
					ObjectUtils.asMap("resource_type", "image", "type", "upload"));

			deleted += countDeleted(res);
			more = Boolean.TRUE.equals(res.get("partial"));
			rounds++;
		} while (more && rounds < 100);

		return deleted;
	}

	private int countDeleted(Map<String, Object> response) {
		Object deleted = response.get("deleted");
		if (!(deleted instanceof Map<?, ?> map)) {
			return 0;
		}

		return (int) map.values().stream().filter("deleted"::equals).count();
	}
}
