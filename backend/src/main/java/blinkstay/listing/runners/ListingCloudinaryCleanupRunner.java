package blinkstay.listing.runners;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import blinkstay.listing.repository.ListingImageRepository;
import blinkstay.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "listing.cloudinary-cleanup.enabled", havingValue = "true", matchIfMissing = false)
public class ListingCloudinaryCleanupRunner implements CommandLineRunner {
	private final Cloudinary cloudinary;

	private final ListingRepository listingRepo;

	private final ListingImageRepository listingImageRepo;

	@Value("${listing.cloudinary-cleanup.folder:blinkstay_listings}")
	private String folder;

	@Override
	public void run(String... args) throws Exception {
		long listings = listingRepo.count();
		long images = listingImageRepo.count();

		if (listings > 0 || images > 0) {
			log.info("Listing data exists (listings={}, images={}): Cloudinary folder '{}' left untouched", listings,
					images, folder);
			return;
		}

		log.warn("Listing tables are empty: deleting all images in Cloudinary folder '{}'", folder);

		try {
			int deleted = deleteAllInFolder(folder);
			log.warn("Cloudinary folder '{}' cleaned: {} images deleted", folder, deleted);
		} catch (Exception e) {
			// Never block application startup because of a cleanup failure
			log.error("Failed to clean Cloudinary folder '{}'", folder, e);
		}
	}

	private int deleteAllInFolder(String folder) throws Exception {
		String prefix = folder.endsWith("/") ? folder : folder + "/";
		int deleted = 0;
		int rounds = 0;
		boolean more;

		do {
			Map<String, Object> res = cloudinary.api().deleteResourcesByPrefix(prefix,
					ObjectUtils.asMap("resource_type", "image", "type", "upload"));

			deleted += countDeleted(res);
			more = Boolean.TRUE.equals(res.get("partial")); // Cloudinary deletes up to 1000 per call
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
